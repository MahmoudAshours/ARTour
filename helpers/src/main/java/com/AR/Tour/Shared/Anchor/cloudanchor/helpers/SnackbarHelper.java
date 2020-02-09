
package com.AR.Tour.Shared.Anchor.cloudanchor.helpers;

import android.app.Activity;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.widget.TextView;

/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code, and exposes simpler
 * methods.
 */
public final class SnackbarHelper {
  private static final int BACKGROUND_COLOR = 0xbf323232;
  private Snackbar messageSnackbar;
  private enum DismissBehavior { HIDE, SHOW, FINISH };
  private int maxLines = 2;
  private String lastMessage = "";

  /** Shows a snackbar with a given message. */
  public void showMessage(Activity activity, String message) {
    if (!message.isEmpty() && (messageSnackbar == null || !lastMessage.equals(message))) {
      lastMessage = message;
      show(activity, message, DismissBehavior.HIDE);
    }
  }

  private void show(
      final Activity activity, final String message, final DismissBehavior dismissBehavior) {
    activity.runOnUiThread(() -> {
        messageSnackbar =
            Snackbar.make(
                activity.findViewById(android.R.id.content),
                message, Snackbar.LENGTH_INDEFINITE);
        messageSnackbar.getView().setBackgroundColor(BACKGROUND_COLOR);
        if (dismissBehavior != DismissBehavior.HIDE) {
          messageSnackbar.setAction("Dismiss", v -> messageSnackbar.dismiss());
          if (dismissBehavior == DismissBehavior.FINISH) {
            messageSnackbar.addCallback(
                new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                  @Override
                  public void onDismissed(Snackbar transientBottomBar, int event) {
                    super.onDismissed(transientBottomBar, event);
                    activity.finish();
                  }
                });
          }
        }
        ((TextView)
                messageSnackbar
                    .getView()
                    .findViewById(android.support.design.R.id.snackbar_text))
            .setMaxLines(maxLines);
        messageSnackbar.show();
      });
  }
}