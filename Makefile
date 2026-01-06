GRADLE_WRAPPER := ./gradlew
APK_PATH := app/build/outputs/apk/debug/app-debug.apk
PKG_NAME := com.dagimg.glide
ACTIVITY_NAME := .MainActivity
ADB := adb
ADB_PORT := 5037
DEBUG_PORT := 8600
LOG_TAG := GLIDE
WAIT_TIME := 2
USE_DEBUGGER := 1

ktlint:
	ktlint --format

compile:
	$(GRADLE_WRAPPER) compileDebugKotlin

lint: ktlint

release:
	$(GRADLE_WRAPPER) assembleRelease

build:
	$(GRADLE_WRAPPER) assembleDebug

install: build
	$(ADB) install -r $(APK_PATH)

# Fast deploy method - uses InstallDebug which is faster for smaller changes
# This is what Android Studio uses for "Apply Changes"
fast-deploy:
	$(GRADLE_WRAPPER) installDebug

launch-debug:
	$(ADB) shell am start -D -n $(PKG_NAME)/$(ACTIVITY_NAME)

launch:
	$(ADB) shell am start -n $(PKG_NAME)/$(ACTIVITY_NAME)

# Normal run without clean (preserves build cache, faster)
run: install
	@if [ $(USE_DEBUGGER) -eq 1 ]; then \
		$(MAKE) launch-debug; \
		sleep $(WAIT_TIME); \
		$(MAKE) attach; \
	else \
		$(MAKE) launch; \
	fi

# Fast run - just update code and launch with synchronization
fast-run:
	@echo "Force stopping app..."
	$(MAKE) force-stop
	@echo "Installing app..."
	$(GRADLE_WRAPPER) installDebug
	@if [ $(USE_DEBUGGER) -eq 1 ]; then \
		echo "Starting app with debug flag..."; \
		$(MAKE) launch-debug; \
		echo "Waiting for app to initialize ($(WAIT_TIME) seconds)..."; \
		sleep $(WAIT_TIME); \
		echo "Connecting debugger..."; \
		echo "NOTE: If debugger times out, try: make adb-reset followed by make attach_manual"; \
		$(MAKE) attach; \
	else \
		echo "Starting app normally (no debug)..."; \
		$(ADB) shell am start -n $(PKG_NAME)/$(ACTIVITY_NAME); \
	fi


# Attach debugger with automatic continue (run command)
attach:
	@echo "Attaching debugger to process..."
	@echo "Setting up port forwarding on ADB port $(ADB_PORT)..."
	$(eval PROCESS_ID := $(shell $(ADB) shell ps | grep glide | awk '{print $$2}'))
	@if [ -z "$(PROCESS_ID)" ]; then \
		echo "No running process found. Launch the app first."; \
	else \
		echo "Found process ID: $(PROCESS_ID)"; \
		$(ADB) -P $(ADB_PORT) forward tcp:$(DEBUG_PORT) jdwp:$(PROCESS_ID); \
		echo "Debug port forwarded. Connecting debugger..."; \
		echo "run" > /tmp/jdb_commands.txt; \
		echo "exit" >> /tmp/jdb_commands.txt; \
		timeout 20 jdb -attach localhost:$(DEBUG_PORT) < /tmp/jdb_commands.txt || echo "JDB timed out. Try 'make attach_manual' instead"; \
	fi

fast-run-no-debug:
	@echo "Force stopping app..."
	$(MAKE) force-stop
	@echo "Installing app..."
	$(GRADLE_WRAPPER) installDebug
	@echo "Starting app without debugging..."
	$(MAKE) launch
	@echo "App started successfully."

devices:
	$(ADB) devices -l

ps:
	$(ADB) shell ps | grep glide

# Show all logs in real-time (similar to Flutter logs)
logs:
	$(ADB) logcat

# Show application logs only (filters by package name)
applogs:
	$(ADB) logcat --pid=$(shell $(ADB) shell pidof -s $(PKG_NAME))

# Show logs filtered by tag (more specific filtering)
taglogs:
	$(ADB) logcat $(LOG_TAG):V *:S

# Add this to the top of your Makefile
force-stop:
	$(ADB) shell am force-stop $(PKG_NAME)

# SMS Parser CLI - pass arguments after the target
# Usage: make cli ARGS="--help" or make cli ARGS="cbe \"SMS text here\""
cli:
	$(GRADLE_WRAPPER) :app:runCli --args="$(ARGS)"

# Add this new combined target for fastest development workflow:
dev:
	@echo "Installing app..."
	$(GRADLE_WRAPPER) installDebug
	@echo "Starting app without debugging..."
	$(ADB) shell am start -n $(PKG_NAME)/$(ACTIVITY_NAME)
	@echo "Waiting for app to initialize (1 second)..."
	sleep 1
	@echo "Starting log viewer..."
	$(MAKE) applogs
