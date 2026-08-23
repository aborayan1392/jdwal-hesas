from pathlib import Path
import re

root = Path('work/source/SchoolBellJava')
p = root / 'app/src/main/java/com/aboryan/schoolbell/ui/PeriodCardView.java'
text = p.read_text(encoding='utf-8')
text = text.replace('        titleWrap.addView(timeRow, timeRowParams);', '        addView(timeRow, timeRowParams);')
text = text.replace('        infoRow.setGravity(Gravity.CENTER_VERTICAL);', '        infoRow.setGravity(Gravity.CENTER);')
text = text.replace('        titleWrap.addView(infoRow, infoParams);', '        addView(infoRow, infoParams);')
old = '''        durationView = createInfoChip(context);\n        LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);\n        durationParams.setMargins(0, 0, UiUtils.dp(context, 8), 0);\n        infoRow.addView(durationView, durationParams);\n\n        typeView = createInfoChip(context);\n        infoRow.addView(typeView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));'''
new = '''        typeView = createInfoChip(context);\n        typeView.setMinWidth(UiUtils.dp(context, 170));\n        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);\n        typeParams.setMargins(0, 0, UiUtils.dp(context, 6), 0);\n        infoRow.addView(typeView, typeParams);\n\n        durationView = createInfoChip(context);\n        durationView.setMinWidth(UiUtils.dp(context, 104));\n        LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);\n        durationParams.setMargins(UiUtils.dp(context, 6), 0, 0, 0);\n        infoRow.addView(durationView, durationParams);'''
if old not in text:
    raise SystemExit('Expected v2.2.9 info row block not found')
text = text.replace(old, new)
p.write_text(text, encoding='utf-8')

build = root / 'app/build.gradle'
b = build.read_text(encoding='utf-8')
b = re.sub(r'versionCode\s+\d+', 'versionCode 20', b)
b = re.sub(r'versionName\s+"[^"]+"', 'versionName "2.2.10"', b)
build.write_text(b, encoding='utf-8')
