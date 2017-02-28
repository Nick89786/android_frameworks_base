/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settingslib.drawer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.android.settingslib.SettingLibRobolectricTestRunner;
import com.android.settingslib.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingLibRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class CategoryManagerTest {

    private Context mContext;
    private CategoryManager mCategoryManager;
    private Map<Pair<String, String>, Tile> mTileByComponentCache;
    private Map<String, DashboardCategory> mCategoryByKeyMap;

    @Before
    public void setUp() {
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mTileByComponentCache = new HashMap<>();
        mCategoryByKeyMap = new HashMap<>();
        mCategoryManager = CategoryManager.get(mContext);
    }

    @Test
    public void getInstance_shouldBeSingleton() {
        assertThat(mCategoryManager).isSameAs(CategoryManager.get(mContext));
    }

    @Test
    public void backwardCompatCleanupForCategory_shouldNotChangeCategoryForNewKeys() {
        final Tile tile1 = new Tile();
        final Tile tile2 = new Tile();
        tile1.category = CategoryKey.CATEGORY_ACCOUNT;
        tile2.category = CategoryKey.CATEGORY_ACCOUNT;
        final DashboardCategory category = new DashboardCategory();
        category.addTile(tile1);
        category.addTile(tile2);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_ACCOUNT, category);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "1"), tile1);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "2"), tile2);

        mCategoryManager.backwardCompatCleanupForCategory(mTileByComponentCache, mCategoryByKeyMap);

        assertThat(mCategoryByKeyMap.size()).isEqualTo(1);
        assertThat(mCategoryByKeyMap.get(CategoryKey.CATEGORY_ACCOUNT)).isNotNull();
    }

    @Test
    public void backwardCompatCleanupForCategory_shouldNotChangeCategoryForMixedKeys() {
        final Tile tile1 = new Tile();
        final Tile tile2 = new Tile();
        final String oldCategory = "com.android.settings.category.wireless";
        tile1.category = CategoryKey.CATEGORY_ACCOUNT;
        tile2.category = oldCategory;
        final DashboardCategory category1 = new DashboardCategory();
        category1.addTile(tile1);
        final DashboardCategory category2 = new DashboardCategory();
        category2.addTile(tile2);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_ACCOUNT, category1);
        mCategoryByKeyMap.put(oldCategory, category2);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS1"), tile1);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS2"), tile2);

        mCategoryManager.backwardCompatCleanupForCategory(mTileByComponentCache, mCategoryByKeyMap);

        assertThat(mCategoryByKeyMap.size()).isEqualTo(2);
        assertThat(mCategoryByKeyMap.get(CategoryKey.CATEGORY_ACCOUNT).tiles.size()).isEqualTo(1);
        assertThat(mCategoryByKeyMap.get(oldCategory).tiles.size()).isEqualTo(1);
    }

    @Test
    public void backwardCompatCleanupForCategory_shouldChangeCategoryForOldKeys() {
        final Tile tile1 = new Tile();
        final String oldCategory = "com.android.settings.category.wireless";
        tile1.category = oldCategory;
        final DashboardCategory category1 = new DashboardCategory();
        category1.addTile(tile1);
        mCategoryByKeyMap.put(oldCategory, category1);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS1"), tile1);

        mCategoryManager.backwardCompatCleanupForCategory(mTileByComponentCache, mCategoryByKeyMap);

        // Added 1 more category to category map.
        assertThat(mCategoryByKeyMap.size()).isEqualTo(2);
        // The new category map has CATEGORY_NETWORK type now, which contains 1 tile.
        assertThat(mCategoryByKeyMap.get(CategoryKey.CATEGORY_NETWORK).tiles.size()).isEqualTo(1);
        // Old category still exists.
        assertThat(mCategoryByKeyMap.get(oldCategory).tiles.size()).isEqualTo(1);
    }

    @Test
    public void normalizePriority_singlePackage_shouldReorderBasedOnPriority() {
        // Create some fake tiles that are not sorted.
        final String testPackage = "com.android.test";
        final DashboardCategory category = new DashboardCategory();
        final Tile tile1 = new Tile();
        tile1.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class1"));
        tile1.priority = 100;
        final Tile tile2 = new Tile();
        tile2.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class2"));
        tile2.priority = 50;
        final Tile tile3 = new Tile();
        tile3.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class3"));
        tile3.priority = 200;
        category.tiles.add(tile1);
        category.tiles.add(tile2);
        category.tiles.add(tile3);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_HOMEPAGE, category);

        // Normalize their priorities
        mCategoryManager.normalizePriority(ShadowApplication.getInstance().getApplicationContext(),
                mCategoryByKeyMap);

        // Verify they are now sorted.
        assertThat(category.tiles.get(0)).isSameAs(tile2);
        assertThat(category.tiles.get(1)).isSameAs(tile1);
        assertThat(category.tiles.get(2)).isSameAs(tile3);
        // Verify their priority is normalized
        assertThat(category.tiles.get(0).priority).isEqualTo(0);
        assertThat(category.tiles.get(1).priority).isEqualTo(1);
        assertThat(category.tiles.get(2).priority).isEqualTo(2);
    }

    @Test
    public void normalizePriority_multiPackage_shouldReorderBasedOnPackageAndPriority() {
        // Create some fake tiles that are not sorted.
        final String testPackage1 = "com.android.test1";
        final String testPackage2 = "com.android.test2";
        final DashboardCategory category = new DashboardCategory();
        final Tile tile1 = new Tile();
        tile1.intent =
                new Intent().setComponent(new ComponentName(testPackage2, "class1"));
        tile1.priority = 100;
        final Tile tile2 = new Tile();
        tile2.intent =
                new Intent().setComponent(new ComponentName(testPackage1, "class2"));
        tile2.priority = 100;
        final Tile tile3 = new Tile();
        tile3.intent =
                new Intent().setComponent(new ComponentName(testPackage1, "class3"));
        tile3.priority = 50;
        category.tiles.add(tile1);
        category.tiles.add(tile2);
        category.tiles.add(tile3);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_HOMEPAGE, category);

        // Normalize their priorities
        mCategoryManager.normalizePriority(ShadowApplication.getInstance().getApplicationContext(),
                mCategoryByKeyMap);

        // Verify they are now sorted.
        assertThat(category.tiles.get(0)).isSameAs(tile3);
        assertThat(category.tiles.get(1)).isSameAs(tile2);
        assertThat(category.tiles.get(2)).isSameAs(tile1);
        // Verify their priority is normalized
        assertThat(category.tiles.get(0).priority).isEqualTo(0);
        assertThat(category.tiles.get(1).priority).isEqualTo(1);
        assertThat(category.tiles.get(2).priority).isEqualTo(2);
    }

    @Test
    public void normalizePriority_internalPackageTiles_shouldSkipTileForInternalPackage() {
        // Create some fake tiles that are not sorted.
        final String testPackage =
                ShadowApplication.getInstance().getApplicationContext().getPackageName();
        final DashboardCategory category = new DashboardCategory();
        final Tile tile1 = new Tile();
        tile1.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class1"));
        tile1.priority = 100;
        final Tile tile2 = new Tile();
        tile2.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class2"));
        tile2.priority = 100;
        final Tile tile3 = new Tile();
        tile3.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class3"));
        tile3.priority = 50;
        category.tiles.add(tile1);
        category.tiles.add(tile2);
        category.tiles.add(tile3);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_HOMEPAGE, category);

        // Normalize their priorities
        mCategoryManager.normalizePriority(ShadowApplication.getInstance().getApplicationContext(),
                mCategoryByKeyMap);

        // Verify the sorting order is not changed
        assertThat(category.tiles.get(0)).isSameAs(tile1);
        assertThat(category.tiles.get(1)).isSameAs(tile2);
        assertThat(category.tiles.get(2)).isSameAs(tile3);
        // Verify their priorities are not changed.
        assertThat(category.tiles.get(0).priority).isEqualTo(100);
        assertThat(category.tiles.get(1).priority).isEqualTo(100);
        assertThat(category.tiles.get(2).priority).isEqualTo(50);
    }

    @Test
    public void filterTiles_noDuplicate_noChange() {
        // Create some unique tiles
        final String testPackage =
                ShadowApplication.getInstance().getApplicationContext().getPackageName();
        final DashboardCategory category = new DashboardCategory();
        final Tile tile1 = new Tile();
        tile1.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class1"));
        tile1.priority = 100;
        final Tile tile2 = new Tile();
        tile2.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class2"));
        tile2.priority = 100;
        final Tile tile3 = new Tile();
        tile3.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class3"));
        tile3.priority = 50;
        category.tiles.add(tile1);
        category.tiles.add(tile2);
        category.tiles.add(tile3);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_HOMEPAGE, category);

        mCategoryManager.filterDuplicateTiles(mCategoryByKeyMap);

        assertThat(category.tiles.size()).isEqualTo(3);
    }

    @Test
    public void filterTiles_hasDuplicate_shouldOnlyKeepUniqueTiles() {
        // Create tiles pointing to same intent.
        final String testPackage =
                ShadowApplication.getInstance().getApplicationContext().getPackageName();
        final DashboardCategory category = new DashboardCategory();
        final Tile tile1 = new Tile();
        tile1.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class1"));
        tile1.priority = 100;
        final Tile tile2 = new Tile();
        tile2.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class1"));
        tile2.priority = 100;
        final Tile tile3 = new Tile();
        tile3.intent =
                new Intent().setComponent(new ComponentName(testPackage, "class1"));
        tile3.priority = 50;
        category.tiles.add(tile1);
        category.tiles.add(tile2);
        category.tiles.add(tile3);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_HOMEPAGE, category);

        mCategoryManager.filterDuplicateTiles(mCategoryByKeyMap);

        assertThat(category.tiles.size()).isEqualTo(1);
    }
}
