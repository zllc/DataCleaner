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
package org.datacleaner.descriptors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.datacleaner.job.ComponentValidationException;

final class ValidateMethodDescriptorImpl extends AbstractMethodDescriptor implements ValidateMethodDescriptor {

    private static final long serialVersionUID = 1L;

    public ValidateMethodDescriptorImpl(Method method, ComponentDescriptor<?> componentDescriptor) {
        super(method, componentDescriptor);
    }

    @Override
    public void validate(Object component) throws ComponentValidationException {
        invoke(component);
    }

    @Override
    protected RuntimeException convertThrownException(Object component, InvocationTargetException e) {
        return new ComponentValidationException(getComponentDescriptor(), component, e.getCause());
    }
}
