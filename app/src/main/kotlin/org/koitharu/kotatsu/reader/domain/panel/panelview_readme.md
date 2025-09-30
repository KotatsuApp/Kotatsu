# Panel View Module Notes

## Purpose
This module enables panel-aware reading in Kotatsu. It now covers three areas:
- **Detection** – an adaptive detector that downsamples images, segments panels, and caches results per page.
- **Pipeline integration** – page loading kicks off detection and exposes results through the reader view model.
- **Presentation** – a controller animates the `SubsamplingScaleImageView`, while an overlay highlights the active panel.

## Key Components
- `PanelDetection.kt` – exposes the geometry model (`Panel`, `PanelSequence`, etc.) and the new `AdaptivePanelDetector`.
- `PageLoader.detectPanels()` – decodes scaled bitmaps, calls the detector, caches `PanelDetectionResult`, and reorders panels for the requested flow.
- `PageViewModel` – launches detection when pages load or when panel mode is toggled, publishing results via `panelState`.
- `BasePageHolder` + `PanelZoomController` – subscribe to detection results, animate zoom/centering, and manage the highlight overlay (`PanelHighlightOverlay`).
- Settings – a dedicated toggle (`reader_panel_mode`) is available both in the quick reader sheet and the full settings screen.

## Current Structure
- **Domain**: `PanelDetection.kt`, `AdaptivePanelDetectorTest.kt`.
- **Reader pipeline**: `PageLoader.kt`, `PageViewModel.kt`.
- **UI**: `PanelZoomController.kt`, `PanelHighlightOverlay.kt`, updates to `BasePageHolder`, `PageHolder`, and XML layouts.
- **Config**: `ReaderSettings.kt`, `ReaderConfigSheet`, `pref_reader.xml`, `AppSettings` (new key), plus Hilt provider in `AppModule`.

## Status
- ? Adaptive detector implemented with Otsu thresholding, connected-component labeling, merging, and fallbacks.
- ? Detection wired into `PageLoader` (bitmap sampling + caching) and `PageViewModel` (state flow + refresh hooks).
- ? Reader UI integrated (settings toggle, quick sheet, overlay rendering, zoom controller).
- ?? Pending: tune flow direction (currently defaults to left-to-right), add richer heuristics for double pages, wire navigation gestures to panel stepping, and add instrumentation coverage.

## Files Updated / Added
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/domain/panel/PanelDetection.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/domain/panel/PanelHighlightOverlay.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/domain/panel/PanelZoomController.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/domain/PageLoader.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/ui/pager/vm/PageViewModel.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/ui/pager/BasePageHolder.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/ui/pager/standard/PageHolder.kt`
- `app/src/main/res/layout/item_page.xml`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/ui/config/ReaderConfigSheet.kt`
- `app/src/main/res/layout/sheet_reader_config.xml`
- `app/src/main/res/xml/pref_reader.xml`
- `app/src/main/kotlin/org/koitharu/kotatsu/core/prefs/AppSettings.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/ui/config/ReaderSettings.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/kotlin/org/koitharu/kotatsu/core/AppModule.kt`
- `app/src/main/kotlin/org/koitharu/kotatsu/reader/domain/panel/AdaptivePanelDetectorTest.kt`

## Next Steps
1. Derive panel ordering and flow from the active reader mode rather than assuming left-to-right.
2. Expose navigation hooks (next/previous panel) via tap grid or hardware actions.
3. Persist/cached panel sequences across orientation changes and invalidations.
4. Extend unit coverage with fixture-based tests and add instrumentation smoke tests for the reader experience.

## Verification
- Unit tests: `AdaptivePanelDetectorTest` (synthetic cases). Run with `./gradlew testDebugUnitTest`.
- Manual: enable “Panel view” in reader settings, load a manga page with multiple panels, verify automatic zoom and highlight overlay, and confirm fallback to full-page when detectors fail.

_Last updated: 2025-09-30_
