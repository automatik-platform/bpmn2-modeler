/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.runtime.automatik.property;

import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.ui.property.tasks.ActivityDetailComposite;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Bob Brodt
 *
 */
public class JbpmActivityDetailComposite extends ActivityDetailComposite {
	
	/**
	 * @param section
	 */
	public JbpmActivityDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	public JbpmActivityDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void cleanBindings() {
		super.cleanBindings();
	}

	@Override
	public void createBindings(EObject be) {
		super.createBindings(be);
	}
	
	protected void bindReference(Composite parent, EObject object, EReference reference) {
		if ("loopCharacteristics".equals(reference.getName())) { //$NON-NLS-1$
			if (!isModelObjectEnabled(object.eClass(), reference))
				return;
			super.bindReference(parent, object, reference);
			addStandardLoopButton.setVisible(false);
			
		}
		else
			super.bindReference(parent, object, reference);
	}
	
}
