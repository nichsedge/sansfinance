.PHONY: help run install logs devices compile build release test test-unit test-android check-deps update-deps backup restore sync r2-list r2-pull r2-restore push-portfolio backfill-portfolio prune-portfolio inspect-db compact clean

DEVICE ?=
ARGS ?=

help:
	@echo "Sans Finance Makefile"
	@echo "Usage: make [target] [DEVICE=<serial_or_ip_port>] [ARGS=...]"
	@echo ""
	@echo "Device Deployment Targets:"
	@echo "  run                Build, install, and launch on connected device"
	@echo "  install            Build and install APK on device (skip launch)"
	@echo "  logs               Stream live logs filtered for SansFinance"
	@echo "  devices            List all connected USB, Wireless, and Tailscale devices"
	@echo ""
	@echo "Build & Test Targets:"
	@echo "  compile            Rapid Kotlin compilation check"
	@echo "  build              Build debug APK"
	@echo "  release            Build and package release APK"
	@echo "  test               Run all unit & android instrumentation tests"
	@echo "  test-unit          Run JVM unit tests"
	@echo "  test-android       Run instrumentation tests on connected device"
	@echo "  check-deps         Check for library updates (Maven & Google)"
	@echo "  update-deps        Update gradle/libs.versions.toml automatically"
	@echo ""
	@echo "Data & Cloud Storage (R2) Targets:"
	@echo "  backup             Extract database snapshot from phone via ADB"
	@echo "  restore            Restore local database snapshot to phone via ADB"
	@echo "  sync               Bidirectional sync script (App -> PC -> App)"
	@echo "  r2-list            List all database backups in Cloudflare R2 bucket"
	@echo "  r2-pull            Download latest SQLite snapshot from Cloudflare R2"
	@echo "  r2-restore         Download from R2, verify validity, and restore to device"
	@echo "  inspect-db         Inspect tables and stats in SQLite database snapshot"
	@echo "  push-portfolio     Push portfolio JSON snapshots to phone"
	@echo "  backfill-portfolio Run portfolio backfill script"
	@echo "  prune-portfolio    Run portfolio monthly pruning script"
	@echo "  clean              Clean build artifacts"

run:
	bash scripts/run_on_device.sh $(DEVICE)

install:
	INSTALL_ONLY=1 bash scripts/run_on_device.sh $(DEVICE)

logs:
	bash scripts/device_logs.sh $(DEVICE)

devices:
	bash scripts/list_devices.sh

compile:
	./gradlew compileDebugKotlin

build:
	./gradlew assembleDebug

release:
	bash scripts/build_release.sh

test: test-unit test-android

test-unit:
	./gradlew testDebugUnitTest

test-android:
	./gradlew connectedDebugAndroidTest

check-deps:
	./gradlew dependencyUpdates

update-deps:
	./gradlew versionCatalogUpdate

backup:
	bash scripts/backup.sh $(DEVICE)

restore:
	bash scripts/restore.sh $(DEVICE)

sync:
	bash scripts/sync.sh $(DEVICE)

r2-list:
	python3 scripts/pull_cloud_db.py --list

r2-pull:
	python3 scripts/pull_cloud_db.py $(ARGS)

r2-restore:
	bash scripts/restore_from_r2.sh $(ARGS)

inspect-db:
	python3 scripts/inspect_db.py $(ARGS)

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
