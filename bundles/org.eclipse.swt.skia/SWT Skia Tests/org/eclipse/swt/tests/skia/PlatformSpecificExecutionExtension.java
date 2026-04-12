package org.eclipse.swt.tests.skia;

import static org.junit.Assume.assumeTrue;

import org.eclipse.swt.SWT;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class PlatformSpecificExecutionExtension implements BeforeAllCallback {
	private PlatformSpecificExecutionExtension()  {
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		assumeTrue("test is specific for win or gtk", isFittingOS());
		assumeTrue("architecture of platform is x86_64", isFittingArchitecture());
	}

	private static boolean isFittingOS() {
		return "win32".equals(SWT.getPlatform()) || "gtk".equals(SWT.getPlatform());
	}

	private static boolean isFittingArchitecture() {
		final var arc = arch();
		return "x86_64".equals(arc);
	}

	static String arch() {
		final String osArch = System.getProperty("os.arch"); //$NON-NLS-1$
		if (osArch.equals ("amd64")) { //$NON-NLS-1$
			return "x86_64";
		}
		return osArch;
	}

}