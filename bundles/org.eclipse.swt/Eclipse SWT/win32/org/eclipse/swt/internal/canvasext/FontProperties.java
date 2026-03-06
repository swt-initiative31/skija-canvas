package org.eclipse.swt.internal.canvasext;

import java.util.*;

import org.eclipse.swt.graphics.*;

public class FontProperties {

	// Standard OpenType stretch values
	private static final Map<String, Integer> STRETCH_MAP = new HashMap<>();
	private static final Set<String> REGION_KEYS = new HashSet<>();

	static {
		STRETCH_MAP.put("UltraCondensed", 1); // 50.0%
		STRETCH_MAP.put("ExtraCondensed", 2); // 62.5%
		STRETCH_MAP.put("Condensed", 3); // 75.0%
		STRETCH_MAP.put("SemiCondensed", 4); // 87.5%
		STRETCH_MAP.put("Normal", 5); // 100.0%
		STRETCH_MAP.put("Medium", 5);
		STRETCH_MAP.put("SemiExpanded", 6); // 112.5%
		STRETCH_MAP.put("Expanded", 7); // 125.0%
		STRETCH_MAP.put("ExtraExpanded", 8); // 150.0%
		STRETCH_MAP.put("UltraExpanded", 9); // 200.0%


		REGION_KEYS.add(" Greek");
		REGION_KEYS.add(" TUR");
		REGION_KEYS.add(" Baltic");
		REGION_KEYS.add(" CE");
		REGION_KEYS.add(" CYR");
		REGION_KEYS.add(" Transparent");
	}





	public int lfHeight;
	public int lfWidth;
	public int lfEscapement;
	public int lfOrientation;
	public int lfWeight;
	public byte lfItalic;
	public byte lfUnderline;
	public byte lfStrikeOut;
	public String name;

	private FontProperties() {

	}

	private static FontProperties getFontProperties(FontData fd) {
		var fp = new FontProperties();
		var d = fd.data;

		String name = fd.getName();

		for(String local : REGION_KEYS) {
			if(name.endsWith(local)) {
				name = name.substring(0, name.length() - local.length());
				break;
			}

		}

		var fontName = analyzeManual(name);

		fp.name = fontName.baseName;
		fp.lfHeight = fd.getHeight();
		fp.lfItalic = d.lfItalic;
		fp.lfEscapement = d.lfEscapement;
		fp.lfOrientation = d.lfOrientation;
		fp.lfStrikeOut = d.lfStrikeOut;
		fp.lfUnderline = d.lfUnderline;
		if(d.lfWeight == 0)
			fp.lfWeight = 400; // Normal weight
		else
			fp.lfWeight = d.lfWeight;
		var stretch = STRETCH_MAP.get(fontName.detectedStretch);
		if(stretch != null)
			fp.lfWidth = stretch;

		return fp;
	}

	public static FontProperties getFontProperties(Font font) {
		return getFontProperties(font.getFontData()[0]);
	}

	private static FontName analyzeManual(String fullDescription) {
		// Bekannte Stretch-Keywords definieren
		String[] stretchKeywords = { "UltraCondensed", "ExtraCondensed", "Condensed", "SemiCondensed", "SemiExpanded",
				"Expanded" };

		String baseName = fullDescription;
		String detectedStretch = null;

		for (String keyword : stretchKeywords) {
			if (fullDescription.contains(keyword)) {
				detectedStretch = keyword;
				baseName = fullDescription;
				break;
			}
		}

		return new FontName(baseName, detectedStretch);
	}

	private record FontName(String baseName, String detectedStretch) {
	}

}
