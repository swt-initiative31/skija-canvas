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

import java.util.*;

import org.eclipse.swt.*;

public class ExternalCanvasHandler {

	private static IExternalCanvasFactory externalFactory = ServiceLoader.load(IExternalCanvasFactory.class).findFirst().orElse(null);

	public static boolean isActive(Canvas canvas, int style) {

		if((style & SWT.SKIA) != 0 && externalFactory != null)
			return true;
		return false;
	}

	public static IExternalCanvasHandler createHandler(Canvas c) {

		return externalFactory.createCanvasExtension(c);

	}

}
