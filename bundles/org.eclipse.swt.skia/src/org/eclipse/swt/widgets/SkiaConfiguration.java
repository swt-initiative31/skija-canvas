/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.swt.widgets;

public class SkiaConfiguration {

	public static void activateSkiaRaster() {
		ExternalCanvasHandler.setExternalCanvasFactory(SkiaRasterCanvasExtension::new);
	}

	public static void activateSkiaGl() {
		ExternalCanvasHandler.setExternalCanvasFactory(SkiaGlCanvasExtension::new);
	}

	public static void resetCanvasConfiguration() {
		ExternalCanvasHandler.setExternalCanvasFactory(null);
	}

}
