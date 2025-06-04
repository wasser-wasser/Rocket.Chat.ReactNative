#!/usr/bin/env bash

sed -i '' \
	's/PRODUCT_BUNDLE_IDENTIFIER = fdroid_up.rocket.reactnative.ShareExtension;/PRODUCT_BUNDLE_IDENTIFIER = fdroid_up.rocket.ios.Rocket-Chat-ShareExtension;/' \
	../RocketChatRN.xcodeproj/project.pbxproj

sed -i '' \
	's/PRODUCT_BUNDLE_IDENTIFIER = fdroid_up.rocket.reactnative.NotificationService;/PRODUCT_BUNDLE_IDENTIFIER = fdroid_up.rocket.ios.NotificationService;/' \
	../RocketChatRN.xcodeproj/project.pbxproj