/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.graphics;

import java.util.Objects;

public class SkiaFontMetrics {

	private final io.github.humbleui.skija.FontMetrics metrics;

	SkiaFontMetrics(io.github.humbleui.skija.FontMetrics metrics) {
		this.metrics = metrics;
	}

	public int getAscent() {
		// in skija, these are negative usually.
		return Math.abs(DPIScaler.autoScaleDownToInt(metrics.getAscent()));
	}

	public int getDescent() {
		return DPIScaler.autoScaleDownToInt(metrics.getDescent());
	}

	public int getHeight() {
		return DPIScaler.autoScaleDownToInt(metrics.getHeight());
	}

	public int getLeading() {
		return DPIScaler.autoScaleDownToInt(metrics.getLeading());
	}

	public int getAverageCharWidth() {
		return DPIScaler.autoScaleDownToInt(metrics.getAvgCharWidth());
	}

	@Override
	public int hashCode() {
		return Objects.hash(metrics);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SkiaFontMetrics other = (SkiaFontMetrics) obj;
		return Objects.equals(metrics, other.metrics);
	}

	public double getAverageCharacterWidth() {
		return metrics.getAvgCharWidth();
	}
}
