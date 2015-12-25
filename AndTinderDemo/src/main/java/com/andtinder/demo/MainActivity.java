/**
 * AndTinder v0.1 for Android
 *
 * @Author: Enrique López Mañas <eenriquelopez@gmail.com>
 * http://www.lopez-manas.com
 *
 * TAndTinder is a native library for Android that provide a
 * Tinder card like effect. A card can be constructed using an
 * image and displayed with animation effects, dismiss-to-like
 * and dismiss-to-unlike, and use different sorting mechanisms.
 *
 * AndTinder is compatible with API Level 13 and upwards
 *
 * @copyright: Enrique López Mañas
 * @license: Apache License 2.0
 */

package com.andtinder.demo;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.andtinder.model.CardModel;
import com.andtinder.view.StackListView;
import com.andtinder.view.CardAdapter;

public class MainActivity extends Activity {

    /**
     * This variable is the container that will host our cards
     * dev 分支修复bug
     */
	private StackListView mStackListView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mainlayout);

		mStackListView = (StackListView) findViewById(R.id.layoutview);

		Resources r = getResources();

		CardAdapter adapter = new CardAdapter(this);

		adapter.add(new CardModel("Description goes here1", r.getDrawable(R.drawable.picture1)));
		adapter.add(new CardModel("Description goes here2", r.getDrawable(R.drawable.picture2)));
		adapter.add(new CardModel("Description goes here3", r.getDrawable(R.drawable.picture3)));
		adapter.add(new CardModel("Description goes here4", r.getDrawable(R.drawable.picture1)));
		adapter.add(new CardModel("Description goes here5", r.getDrawable(R.drawable.picture2)));
		adapter.add(new CardModel("Description goes here6", r.getDrawable(R.drawable.picture3)));
		adapter.add(new CardModel("Description goes here7", r.getDrawable(R.drawable.picture1)));
		adapter.add(new CardModel("Description goes here8", r.getDrawable(R.drawable.picture2)));

        CardModel cardModel = new CardModel("Description goes here9", r.getDrawable(R.drawable.picture3));
        cardModel.setOnClickListener(new CardModel.OnClickListener() {
           @Override
           public void OnClickListener() {
               Log.i("Swipeable Cards","I am pressing the card");
           }
        });

        cardModel.setOnCardDismissedListener(new CardModel.OnCardDismissedListener() {
            @Override
            public void onLike() {
                Log.i("Swipeable Cards","I like the card");
            }

            @Override
            public void onDislike() {
                Log.i("Swipeable Cards","I dislike the card");
            }
        });

        adapter.add(cardModel);

		mStackListView.setAdapter(adapter);
	}
}
