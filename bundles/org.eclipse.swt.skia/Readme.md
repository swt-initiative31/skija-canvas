org.eclipse.swt.skia
===============

Skia plug-in for the SWT user interface library.


Building and Testing locally:
-----------------------------

From Eclipse:

* If necessary, install the **M2E - Maven integration for Eclipse** (see [m2e](https://eclipse.dev/m2e/))

Import the plugins org.eclipse.swt and org.eclipse.swt.skia and the fragments for your platform (e.g. org.eclipse.swt.gtk.linux.x86_64) into your workspace.

Right-click on the pom.xml in org.eclipse.swt and select `Run As` -> `Maven install`.
Right-click on the pom.xml in org.eclipse.swt.ska and select `Run As` -> `Maven install`.

The second run might fail. If so, close the skia project and reopen it. Then run Maven install again on the skia project. Now it should work.

The Maven run will load the skija dependencies in the lib folder of the skia plugin.