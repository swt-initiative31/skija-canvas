package org.eclipse.swt.tests.skia;

public class SupportedTestPlatform {

	public static boolean isSupported() {
		String platform = org.eclipse.swt.SWT.getPlatform();
		return platform.startsWith("gtk3") || platform.startsWith("win32");
	}

}
