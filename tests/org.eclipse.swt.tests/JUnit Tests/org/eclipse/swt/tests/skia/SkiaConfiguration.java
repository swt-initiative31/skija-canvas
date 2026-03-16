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

public class SkiaConfiguration {

    public static final int SKIA = 1 << 23;


    public static final String RASTER = "raster"; //$NON-NLS-1$
    public static final String OPENGL = "opengl"; //$NON-NLS-1$

    public static final String SKIA_PROPERTY = "org.eclipse.swt.skia.configuration"; //$NON-NLS-1$


    public static void activateSkiaGl() {
	System.setProperty("org.eclipse.swt.skia.configuration", OPENGL);
    }

    public static void resetCanvasConfiguration() {
	System.setProperty("org.eclipse.swt.skia.configuration", "");
    }

}
