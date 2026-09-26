.PHONY: help run logs devices build release test test-unit test-android check-deps update-deps backup restore sync push-portfolio backfill-portfolio prune-portfolio clean

DEVICE ?=

help:
	@echo "Sans Finance Makefile"
	@echo "Usage: make [target] [DEVICE=<serial_or_ip_port>]"
	@echo ""
	@echo "Device Targets:"
	@echo "  run                Build, install, and run on connected device"
	@echo "  logs               Stream live logs filtered for SansFinance"
	@echo "  devices            List all connected USB, Wireless, and Tailscale devices"
	@echo ""
	@echo "Build & Test Targets:"
	@echo "  build              Build debug APK"
	@echo "  release            Build and package release APK"
	@echo "  test               Run all tests"
	@echo "  test-unit          Run JVM unit tests"
	@echo "  test-android       Run instrumentation tests"
	@echo "  check-deps         Check for library updates (Maven & Google)"
	@echo "  update-deps        Update gradle/libs.versions.toml automatically"
	@echo ""
	@echo "Data & Sync Targets:"
	@echo "  backup             Extract database snapshot from phone"
	@echo "  restore            Restore database snapshot to phone"
	@echo "  sync               Run sync script (App -> PC -> App)"
	@echo "  push-portfolio     Push portfolio JSON snapshots to phone"
	@echo "  backfill-portfolio Run portfolio backfill script"
	@echo "  prune-portfolio    Run portfolio monthly pruning script"
	@echo "  clean              Clean build artifacts"

run:
	bash scripts/run_on_device.sh $(DEVICE)

logs:
	bash scripts/device_logs.sh $(DEVICE)

devices:
	bash scripts/list_devices.sh

build:
	./gradlew assembleDebug

release:
	bash scripts/build_release.sh

test: test-unit test-android

test-unit:
	./gradlew testDebugUnitTest --no-configuration-cache

test-android:
	./gradlew connectedDebugAndroidTest

check-deps:
	./gradlew dependencyUpdates --no-configuration-cache

update-deps:
	./gradlew versionCatalogUpdate --no-configuration-cache


backup:
	bash scripts/backup.sh $(DEVICE)

restore:
	bash scripts/restore.sh $(DEVICE)

sync:
	bash scripts/sync.sh $(DEVICE)

push-portfolio:
	bash scripts/push_portfolio.sh $(DEVICE) $(ARGS)

backfill-portfolio:
	bash scripts/backfill_portfolio.sh

prune-portfolio:
	bash scripts/prune_portfolio.sh $(ARGS)

compact: prune-portfolio

clean:
	./gradlew clean
	rm -rf release/
