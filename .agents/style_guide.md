# Brand & Style Guide

This guide outlines the visual identity and design system. Use these specifications to ensure consistency across other platforms and applications.

## 1. Brand Identity
- **Concept**: Clean, modern, and data-driven tracking.

## 2. Color Palette
The color system is based on a refined palette (similar to Tailwind CSS) with a focus on Indigo and Slate.

### Core Colors
| Role | Color Name | Hex Code | Purpose |
| :--- | :--- | :--- | :--- |
| **Primary** | Indigo 500 | `#6366F1` | Primary actions, Dark mode highlights |
| **Primary (Darker)** | Indigo 600 | `#4F46E5` | Light mode primary actions |
| **Primary (Darkest)** | Indigo 700 | `#4338CA` | Hover/Pressed states |
| **Tertiary** | Emerald 500 | `#10B981` | Success states, accents |
| **Error** | Rose 500 | `#F43F5E` | Destructive actions, error messages |
| **Warning** | Amber 500 | `#F59E0B` | Warning indicators |

### Neutral Palette (Slate)
Used for backgrounds, surfaces, and text.
- **Slate 50**: `#F8FAFC` (Lightest background)
- **Slate 100**: `#F1F5F9`
- **Slate 200**: `#E2E8F0`
- **Slate 300**: `#CBD5E1`
- **Slate 400**: `#94A3B8`
- **Slate 500**: `#64748B` (Secondary text)
- **Slate 600**: `#475569`
- **Slate 700**: `#334155`
- **Slate 800**: `#1E293B` (Surface color in Dark Mode)
- **Slate 900**: `#0F172A` (Background color in Dark Mode, Primary text in Light Mode)

## 3. Typography
Atracker uses the default system font family with precise scaling and weights.

| Style | Weight | Size | Line Height | Letter Spacing |
| :--- | :--- | :--- | :--- | :--- |
| **Headline Large** | Bold | 32sp | 40sp | -0.5sp |
| **Headline Medium**| Bold | 24sp | 32sp | -0.5sp |
| **Title Large** | SemiBold | 20sp | 28sp | 0sp |
| **Title Medium** | SemiBold | 18sp | 24sp | 0.15sp |
| **Body Large** | Normal | 16sp | 24sp | 0.5sp |
| **Body Medium** | Normal | 14sp | 20sp | 0.25sp |
| **Label Large** | Medium | 14sp | 20sp | 0.1sp |

## 4. Shapes & Elevation
Atracker emphasizes soft, rounded corners for a modern feel.

- **Small**: 12dp (Buttons, chips)
- **Medium**: 18dp (Cards, small dialogs)
- **Large**: 24dp (Main containers, large cards)
- **Extra Large**: 32dp (Bottom sheets, prominent surfaces)

## 5. UI Themes
The application implements both Light and Dark modes:

### Light Mode
- **Background**: `Slate 50`
- **Surface**: `White`
- **Text (Primary)**: `Slate 900`
- **Text (Secondary)**: `Slate 500`

### Dark Mode
- **Background**: `Slate 900`
- **Surface**: `Slate 800`
- **Text (Primary)**: `Slate 50`
- **Text (Secondary)**: `Slate 500`
