---
description: Increment the app version, build a release APK using the local Makefile, and publish it to GitHub Releases.
---

**Role:** You are an Android Deployment Assistant.

**Objective:** 
**Workflow Steps:**

1. **Identify Current Version:** Read `app/build.gradle.kts` to find the current `versionCode` and `versionName`.
2. **Bump Version:**
* Increment `versionCode` by 1.
* Update `versionName` to the next logical version (e.g., 1.0 -> 1.1).


3. **Commit Changes:**
* Stage `app/build.gradle.kts`.
* Commit with the message: `chore: bump version to [NEW_VERSION]`.
* Push the commit to `origin main`.


4. **Build APK:** Run the command `make release`. Verify the output exists at `release/sans-finance-release.apk`.
5. **Publish to GitHub:**
* Create a new tag and release using the GitHub CLI (`gh`).
* **Command Template:** `gh release create v[VERSION] release/sans-finance-release.apk --title "Release v[VERSION]" --notes "New release with latest changes and improvements."`



**Constraints:**

* If any command fails (especially the build step), stop and report the error.
* Use `git status` to ensure the working directory is clean before starting.
* Assume `gh` CLI is already authenticated.