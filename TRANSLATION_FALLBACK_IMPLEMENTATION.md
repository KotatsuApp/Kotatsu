# Translation Fallback Implementation for Kotatsu

## Overview

This document describes the implementation of the automatic translation source fallback feature for Kotatsu, as requested in GitHub issue #1552. This feature provides seamless reading experience when manga chapters are missing from the primary translation by automatically switching to alternative translations.

## Feature Implementation

### Core Components

#### 1. **TranslationFallbackManager** (`app/src/main/kotlin/org/koitharu/kotatsu/reader/domain/TranslationFallbackManager.kt`)
- Central logic for translation fallback decisions
- Manages translation priorities and branch selection
- Provides fallback result with notification information
- Key methods:
  - `findBestAvailableBranch()` - Determines the best translation branch for navigation
  - `getAllAvailableBranches()` - Lists available translations for a manga
  - `getChapterCountByBranch()` - Provides chapter count statistics

#### 2. **User Preferences** (Added to `AppSettings.kt`)
New preference keys and getters:
- `KEY_TRANSLATION_FALLBACK_ENABLED` - Enable/disable the feature
- `KEY_TRANSLATION_PRIMARY/SECONDARY/TERTIARY` - Translation priority order
- `KEY_TRANSLATION_SHOW_NOTIFICATIONS` - Show switch notifications
- `KEY_TRANSLATION_ALLOW_OVERRIDE` - Allow manual translation switching

#### 3. **Enhanced Chapter Navigation** (Modified `ReaderViewModel.kt`)
- Integrated `TranslationFallbackManager` into chapter navigation
- Enhanced `switchChapterBy()` method with fallback logic
- Added translation switch notification events
- Maintains reading position across translation switches

#### 4. **UI Components**

##### Translation Indicator (`TranslationIndicatorView.kt`)
- Custom view extending `ReaderToastView`
- Shows non-intrusive notifications when translations switch
- Auto-hides after 3 seconds
- Positioned at top-center of screen

##### Reader Activity Integration (Modified `ReaderActivity.kt`)
- Added translation indicator to layout
- Integrated notification observer
- Connected to ViewModel events

##### Settings Interface (Modified `ReaderSettingsFragment.kt` and `pref_reader.xml`)
- New "Translation Fallback" preference category
- Dynamic translation selection dropdowns (populated per-manga)
- Organized preference hierarchy with dependencies

### Implementation Details

#### Translation Selection Algorithm

1. **Current Branch Check**: First attempts to find next/previous chapter in current translation
2. **Priority-Based Fallback**: If unavailable, checks user-defined priority order:
   - Primary translation (user's first choice)
   - Secondary translation (backup)
   - Tertiary translation (second backup)
3. **Completion-Based Fallback**: Falls back to branch with most chapters
4. **Graceful Degradation**: Stays in current branch if no alternatives exist

#### User Experience Flow

1. **Setup**: User configures translation preferences in Reader Settings
2. **Reading**: Normal chapter navigation works as before
3. **Gap Detection**: When reaching end of translation, system checks for alternatives
4. **Automatic Switch**: Seamlessly transitions to fallback translation
5. **Notification**: Shows subtle notification about the switch (if enabled)
6. **Continuation**: Reading continues uninterrupted in new translation

### Settings Configuration

#### Reader Settings > Translation Fallback
- **Enable automatic fallback**: Master switch for the feature
- **Primary translation**: Preferred translation source
- **Secondary translation**: First fallback option
- **Tertiary translation**: Second fallback option
- **Show fallback notifications**: Control notification visibility
- **Allow manual override**: Enable manual translation switching

### Technical Benefits

1. **Performance**: Minimal overhead - only activates when reaching translation boundaries
2. **Memory Efficient**: Uses existing chapter loading mechanisms
3. **Extensible**: Easy to add more translation priorities or selection algorithms
4. **Backward Compatible**: Doesn't break existing functionality when disabled
5. **User Control**: Comprehensive configuration options

### Files Modified/Created

#### New Files:
- `TranslationFallbackManager.kt` - Core fallback logic
- `TranslationIndicatorView.kt` - UI notification component
- `TRANSLATION_FALLBACK_IMPLEMENTATION.md` - This documentation

#### Modified Files:
- `AppSettings.kt` - Added preference constants and getters
- `ReaderViewModel.kt` - Enhanced chapter navigation with fallback
- `ReaderActivity.kt` - Added UI integration
- `ReaderSettingsFragment.kt` - Settings interface setup
- `pref_reader.xml` - Preference layout
- `activity_reader.xml` - Added translation indicator view
- `strings.xml` - Translation-related strings

### String Resources Added

All user-facing text is properly internationalized:
- Translation preference titles and summaries
- Notification messages
- Settings descriptions

### Future Enhancements

1. **Smart Recommendations**: ML-based translation quality suggestions
2. **Chapter Gap Analysis**: Visual indicators in chapter lists
3. **Translation Statistics**: Show completion percentages per branch
4. **Bulk Operations**: Apply preferred translations across multiple manga
5. **Source Integration**: Direct integration with translation group preferences

## Usage Instructions

1. **Enable Feature**: Go to Settings > Reader Settings > Translation Fallback
2. **Configure Priorities**: Set your preferred translation order
3. **Start Reading**: Feature works automatically during chapter navigation
4. **Customization**: Adjust notification preferences as desired

This implementation provides the seamless reading experience requested in issue #1552, automatically handling translation gaps while giving users full control over their preferences.