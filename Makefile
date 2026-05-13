.PHONY: help run build release test test-unit test-android backup restore sync push-portfolio clean

help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  run             Build and run on connected device"
	@echo "  build           Build debug APK"
	@echo "  release         Build and package release APK"
	@echo "  test            Run all tests"
	@echo "  test-unit       Run JVM unit tests"
	@echo "  test-android    Run instrumentation tests"
	@echo "  backup          Run backup script"
	@echo "  restore         Run restore script"
	@echo "  sync            Run sync script"
	@echo "  push-portfolio  Run portfolio push script"
	@echo "  clean           Clean build artifacts"

run:
	bash scripts/run_on_device.sh

build:
	./gradlew assembleDebug

release:
	bash scripts/build_release.sh

test: test-unit test-android

test-unit:
	./gradlew testDebugUnitTest

test-android:
	./gradlew connectedDebugAndroidTest

backup:
	bash scripts/backup.sh

restore:
	bash scripts/restore.sh

sync:
	bash scripts/sync.sh

push-portfolio:
	bash scripts/push_portfolio.sh

clean:
	./gradlew clean
	rm -rf release/
