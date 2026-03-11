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
package org.eclipse.swt.internal.canvasext;

import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

/**
 * Handles the creation of external canvas extensions based on the style of the
 * canvas and the availability of an extension factory.
 */
public class ExternalCanvasHandler {

	private static boolean FAILED_WITH_ERRORS = false;

	// TODO Maybe the external canvas loading should be disabled by default and only activated,
	// if the vm argument explicitly enables it.
	private final static String DISABLE_EXTERNAL_CANVAS = "org.eclipse.swt.external.canvas:disabled";

	// this is only for test cases in order to check whether the software works with
	// the canvas extension.
	// NEVER USE THIS IN PRODUCTIVE CODE!!
	private final static String FORCE_ENABLE_EXTERNAL_CANVAS = "org.eclipse.swt.external.canvas:forceEnabled";

	private static IExternalCanvasFactory externalFactory = ServiceLoader.load(IExternalCanvasFactory.class).findFirst()
			.orElse(null);

	public static boolean isActive(Canvas canvas, int style) {

		if(FAILED_WITH_ERRORS)
			return false;

		var disable = System.getProperty(DISABLE_EXTERNAL_CANVAS);
		if (disable != null)
			return false;

		if (canvas instanceof StyledText || canvas instanceof Decorations)
			return false;

		if ((style & SWT.SKIA) != 0 && externalFactory != null)
			return true;

		var forceEnable = System.getProperty(FORCE_ENABLE_EXTERNAL_CANVAS);
		if (forceEnable != null)
			return true;

		return false;
	}

	public static IExternalCanvasHandler createHandler(Canvas c) {

		if(FAILED_WITH_ERRORS)
			return null;

		try {
			// it is possible that the loading of external libraries can fail.
			return externalFactory.createCanvasExtension(c);
		}catch(Throwable t) {
			FAILED_WITH_ERRORS = true;
			t.printStackTrace(System.err);
			return null;
		}
	}
}
