

package com.AR.Tour.Shared.Anchor.cloudanchor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;


/**
 * Main Activity for the Cloud Anchors Codelab.
 *
 * <p>The bulk of the logic resides in the {@link CloudAnchorFragment}. The activity only creates
 * the fragment and attaches it to this Activity.
 */
public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    FragmentManager fm = getSupportFragmentManager();
    Fragment frag = fm.findFragmentById(R.id.fragment_container);
    if (frag == null) {
      frag = new CloudAnchorFragment();
      fm.beginTransaction().add(R.id.fragment_container, frag).commit();
    }
  }
}
