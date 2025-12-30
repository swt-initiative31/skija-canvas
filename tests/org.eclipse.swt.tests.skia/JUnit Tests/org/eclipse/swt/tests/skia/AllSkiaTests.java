/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     SAP SE and others - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.tests.skia;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite(failIfNoTests = false)
@SelectClasses({
	Test_org_eclipse_swt_widgets_SkiaCanvas_Rectangle.class, //
	Test_org_eclipse_swt_widgets_SkiaCanvas_Text.class,//
	Test_org_eclipse_swt_widgets_SkiaCanvas_Text_Simple.class, //
	Test_org_eclipse_swt_widgets_SkiaCanvas_Two.class, //
})
public class AllSkiaTests {

}
