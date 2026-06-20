const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendHeartBeatPush = onDocumentUpdated("users/{userId}", async (event) => {
  const before = event.data.before.data() || {};
  const after = event.data.after.data() || {};
  const beforeCount = Number(before.receivedUnreadBeatCount || 0);
  const afterCount = Number(after.receivedUnreadBeatCount || 0);
  const token = after.fcmToken;

  if (!token || afterCount <= beforeCount) {
    return;
  }

  try {
    await admin.messaging().send({
      token,
      data: {
        type: "heart_beat",
        receivedUnreadBeatCount: String(afterCount)
      },
      android: {
        priority: "high"
      }
    });
  } catch (error) {
    if (error.code === "messaging/registration-token-not-registered") {
      await event.data.after.ref.update({
        fcmToken: admin.firestore.FieldValue.delete(),
        fcmTokenUpdatedAt: admin.firestore.FieldValue.delete()
      });
      return;
    }

    logger.warn("Heart beat push failed.", error);
  }
});
