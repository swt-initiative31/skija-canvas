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

public class SkiaCanvasFactory implements IExternalCanvasFactory {

	@Override
	public IExternalCanvasHandler createCanvasExtension(Canvas c) {

		final var prop = System.getProperty(SkiaConfiguration.SKIA_PROPERTY);
		if(SkiaConfiguration.RASTER.equals(prop)) {
			return new SkiaRasterCanvasExtension(c);
		}
		if(SkiaConfiguration.OPENGL.equals(prop)) {
			return new SkiaGlCanvasExtension(c);
		}
		return null;
	}

}
