/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.widgets.properties;

import javax.swing.DefaultComboBoxModel;

import org.apache.metamodel.schema.MutableSchema;
import org.apache.metamodel.schema.Schema;
import org.apache.metamodel.util.CollectionUtils;
import org.apache.metamodel.util.MutableRef;
import org.datacleaner.connection.Datastore;
import org.datacleaner.connection.DatastoreConnection;
import org.datacleaner.descriptors.ConfiguredPropertyDescriptor;
import org.datacleaner.job.builder.ComponentBuilder;
import org.datacleaner.widgets.DCComboBox;
import org.datacleaner.widgets.DCComboBox.Listener;
import org.datacleaner.widgets.SchemaStructureComboBoxListRenderer;

/**
 * Alternative String property widget, specifically built for components that
 * need a {@link Schema} name drop down.
 * 
 * @author Kasper Sørensen
 */
public class SchemaNamePropertyWidget extends AbstractPropertyWidget<String> {

    private final DCComboBox<Schema> _comboBox;
    private final MutableRef<Datastore> _datastoreRef;

    public SchemaNamePropertyWidget(ComponentBuilder componentBuilder,
            ConfiguredPropertyDescriptor propertyDescriptor) {
        super(componentBuilder, propertyDescriptor);
        _comboBox = new DCComboBox<Schema>();
        _comboBox.setRenderer(new SchemaStructureComboBoxListRenderer(false));
        _comboBox.setEditable(false);
        addComboListener(new Listener<Schema>() {
            @Override
            public void onItemSelected(Schema item) {
                fireValueChanged();
            }
        });
        add(_comboBox);
        _datastoreRef = new MutableRef<Datastore>();

        setValue(getCurrentValue());
    }

    public void addComboListener(Listener<Schema> listener) {
        _comboBox.addListener(listener);
    }

    public void setDatastore(Datastore datastore) {
        String previousValue = getValue();
        _datastoreRef.set(datastore);
        if (datastore == null) {
            _comboBox.setModel(new DefaultComboBoxModel<Schema>(new Schema[1]));
        } else {
            try (final DatastoreConnection con = datastore.openConnection()) {
                Schema[] schemas = con.getSchemaNavigator().getSchemas();
                schemas = CollectionUtils.array(new Schema[1], schemas);
                _comboBox.setModel(new DefaultComboBoxModel<Schema>(schemas));
                Schema newValue = null;
                if (previousValue != null) {
                    newValue = con.getSchemaNavigator().getSchemaByName(previousValue);
                }
                if (newValue == null) {
                    newValue = con.getSchemaNavigator().getDefaultSchema();
                }
                _comboBox.setSelectedItem(newValue);
            }
        }
    }

    @Override
    public String getValue() {
        Schema schema = getSchema();
        if (schema == null) {
            return null;
        }
        return schema.getName();
    }

    public Schema getSchema() {
        Schema schema = _comboBox.getSelectedItem();
        return schema;
    }

    @Override
    protected void setValue(String value) {
        final Datastore datastore = _datastoreRef.get();
        if (value == null && datastore != null) {
            try (DatastoreConnection con = datastore.openConnection()) {
                value = con.getSchemaNavigator().getDefaultSchema().getName();
            }
        }

        if (getValue() == value) {
            return;
        }

        final Schema schema;
        if (value == null) {
            schema = null;
        } else if (datastore == null) {
            schema = new MutableSchema(value);
        } else {
            try (DatastoreConnection con = datastore.openConnection()) {
                schema = con.getSchemaNavigator().getSchemaByName(value);
            }
        }

        _comboBox.setEditable(true);
        _comboBox.setSelectedItem(schema);
        _comboBox.setEditable(false);
    }

    public void connectToTableNamePropertyWidget(final SingleTableNamePropertyWidget tableNamePropertyWidget) {
        addComboListener(new Listener<Schema>() {
            @Override
            public void onItemSelected(Schema item) {
                // update the table name when schema is selected
                tableNamePropertyWidget.setSchema(_datastoreRef.get(), item);
            }
        });
    }

}
