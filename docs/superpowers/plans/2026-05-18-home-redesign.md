# Kế hoạch Triển khai: Nâng cấp Giao diện Danh sách Sản phẩm (Home Screen Redesign)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thay đổi toàn bộ giao diện màn hình danh sách sản phẩm theo dạng tab, thanh lọc ngang, toolbar tìm kiếm cao cấp từ ảnh chụp màn hình, di chuyển chức năng đăng xuất sang tab cài đặt.

**Architecture:** 
- Thêm màu sắc và drawable tùy biến trong resources.
- Chuyển cấu trúc Header màu xanh lá cây cũ thành Toolbar tìm kiếm màu trắng hiện đại có nút Back, Quét mã, Sắp xếp, Chuyển lưới trong `fragment_home.xml`.
- Tách rời logic Lời chào & Đăng xuất sang `fragment_more.xml` / `MoreFragment.java`.
- Cập nhật ViewHolder và binding logic trong `ProductAdapter.java` và `HomeFragment.java`.

---

### Task 1: Thiết lập Màu sắc & Drawable mới
Chúng ta cần thêm các định nghĩa màu mới vào `colors.xml` và tạo các drawable cần thiết cho nền tìm kiếm và nút bấm.

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/drawable/bg_search_round.xml`
- Create: `app/src/main/res/drawable/bg_pill_active.xml`
- Create: `app/src/main/res/drawable/bg_pill_inactive.xml`
- Create: `app/src/main/res/drawable/bg_share_btn.xml`

- [ ] **Step 1: Cập nhật `colors.xml`**
  Thêm các tài nguyên màu sắc mới phục vụ thiết kế:
  ```xml
  <!-- Thêm vào bên trong thẻ <resources> trong file app/src/main/res/values/colors.xml -->
  <color name="colorAccentBlue">#2979FF</color>
  <color name="colorPriceOrange">#E65100</color>
  <color name="colorTabActive">#2E7D32</color>
  <color name="colorPillActiveBorder">#1A73E8</color>
  <color name="colorPillInactiveBg">#F1F3F4</color>
  <color name="colorPillActiveBg">#FFFFFF</color>
  <color name="colorTextDark">#202124</color>
  <color name="colorTextGrey">#70757A</color>
  <color name="colorShareBorder">#DADCE0</color>
  ```

- [ ] **Step 2: Tạo drawable `bg_search_round.xml`**
  Tạo nền bo tròn xám nhạt cho ô nhập Tìm kiếm:
  ```xml
  <!-- File: app/src/main/res/drawable/bg_search_round.xml -->
  <?xml version="1.0" encoding="utf-8"?>
  <shape xmlns:android="http://schemas.android.com/apk/res/android"
      android:shape="rectangle">
      <solid android:color="#F1F3F4"/>
      <corners android:radius="8dp"/>
  </shape>
  ```

- [ ] **Step 3: Tạo drawable nhãn lọc active `bg_pill_active.xml`**
  ```xml
  <!-- File: app/src/main/res/drawable/bg_pill_active.xml -->
  <?xml version="1.0" encoding="utf-8"?>
  <shape xmlns:android="http://schemas.android.com/apk/res/android"
      android:shape="rectangle">
      <solid android:color="@color/colorPillActiveBg"/>
      <stroke
          android:width="1dp"
          android:color="@color/colorPillActiveBorder"/>
      <corners android:radius="18dp"/>
  </shape>
  ```

- [ ] **Step 4: Tạo drawable nhãn lọc inactive `bg_pill_inactive.xml`**
  ```xml
  <!-- File: app/src/main/res/drawable/bg_pill_inactive.xml -->
  <?xml version="1.0" encoding="utf-8"?>
  <shape xmlns:android="http://schemas.android.com/apk/res/android"
      android:shape="rectangle">
      <solid android:color="@color/colorPillInactiveBg"/>
      <corners android:radius="18dp"/>
  </shape>
  ```

- [ ] **Step 5: Tạo drawable nút chia sẻ `bg_share_btn.xml`**
  ```xml
  <!-- File: app/src/main/res/drawable/bg_share_btn.xml -->
  <?xml version="1.0" encoding="utf-8"?>
  <shape xmlns:android="http://schemas.android.com/apk/res/android"
      android:shape="rectangle">
      <solid android:color="#FFFFFF"/>
      <stroke
          android:width="1dp"
          android:color="@color/colorShareBorder"/>
      <corners android:radius="8dp"/>
  </shape>
  ```

- [ ] **Step 6: Tạo icon chia sẻ `ic_share.xml`**
  Tạo vector drawable biểu tượng Share trong thư mục drawable nếu chưa có.
  ```xml
  <!-- File: app/src/main/res/drawable/ic_share.xml -->
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:viewportWidth="24"
      android:viewportHeight="24">
    <path
        android:fillColor="#1A73E8"
        android:pathData="M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7L15.9,7.54c0.54,0.51 1.26,0.83 2.1,0.83c1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3s-3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.78,9 5.9,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.88,0 1.6,-0.31 2.14,-0.81l7.98,4.67c-0.05,0.21 -0.08,0.43 -0.08,0.65c0,1.61 1.31,2.92 2.92,2.92s2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z"/>
  </vector>
  ```

- [ ] **Step 7: Commit**
  ```bash
  git add app/src/main/res/values/colors.xml app/src/main/res/drawable/bg_*.xml app/src/main/res/drawable/ic_share.xml
  git commit -m "style: add custom color and drawable assets for product list revamp"
  ```

---

### Task 2: Redesign layout màn hình chính `fragment_home.xml`
Thay thế toàn bộ code cũ bằng thiết kế Toolbar phẳng, Tabs, và thanh Lọc nằm ngang.

**Files:**
- Modify: `app/src/main/res/layout/fragment_home.xml`

- [ ] **Step 1: Viết lại file `fragment_home.xml`**
  ```xml
  <!-- File: app/src/main/res/layout/fragment_home.xml -->
  <?xml version="1.0" encoding="utf-8"?>
  <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:app="http://schemas.android.com/apk/res-auto"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:background="#F5F5F5">

      <!-- 1. Top Search Bar Toolbar -->
      <LinearLayout
          android:id="@+id/llToolbar"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:orientation="horizontal"
          android:gravity="center_vertical"
          android:paddingHorizontal="12dp"
          android:paddingVertical="8dp"
          android:background="#FFFFFF">

          <ImageButton
              android:id="@+id/btnBack"
              android:layout_width="40dp"
              android:layout_height="40dp"
              android:background="?attr/selectableItemBackgroundBorderless"
              android:src="@android:drawable/abc_ic_ab_back_material"
              app:tint="#555555"
              android:contentDescription="Quay lại" />

          <LinearLayout
              android:layout_width="0dp"
              android:layout_height="40dp"
              android:layout_weight="1"
              android:layout_marginStart="8dp"
              android:layout_marginEnd="8dp"
              android:orientation="horizontal"
              android:gravity="center_vertical"
              android:background="@drawable/bg_search_round"
              android:paddingHorizontal="10dp">

              <ImageView
                  android:layout_width="18dp"
                  android:layout_height="18dp"
                  android:src="@android:drawable/ic_menu_search"
                  app:tint="#757575" />

              <EditText
                  android:id="@+id/etSearch"
                  android:layout_width="0dp"
                  android:layout_height="match_parent"
                  android:layout_weight="1"
                  android:layout_marginStart="6dp"
                  android:layout_marginEnd="6dp"
                  android:background="@android:color/transparent"
                  android:hint="Tìm tên, mã SKU, ..."
                  android:textSize="13sp"
                  android:textColor="@color/colorTextDark"
                  android:textColorHint="#999999"
                  android:inputType="text"
                  android:singleLine="true"
                  android:imeOptions="actionSearch" />

              <ImageButton
                  android:id="@+id/btnScan"
                  android:layout_width="28dp"
                  android:layout_height="28dp"
                  android:background="?attr/selectableItemBackgroundBorderless"
                  android:src="@android:drawable/ic_menu_camera"
                  app:tint="#757575"
                  android:contentDescription="Quét mã vạch" />
          </LinearLayout>

          <ImageButton
              android:id="@+id/btnSort"
              android:layout_width="40dp"
              android:layout_height="40dp"
              android:background="?attr/selectableItemBackgroundBorderless"
              android:src="@android:drawable/ic_menu_sort_by_size"
              app:tint="#555555"
              android:contentDescription="Sắp xếp" />

          <ImageButton
              android:id="@+id/btnGridToggle"
              android:layout_width="40dp"
              android:layout_height="40dp"
              android:background="?attr/selectableItemBackgroundBorderless"
              android:src="@android:drawable/ic_dialog_dialer"
              app:tint="#555555"
              android:contentDescription="Đổi kiểu lưới" />
      </LinearLayout>

      <!-- Divider -->
      <View
          android:id="@+id/divider1"
          android:layout_width="match_parent"
          android:layout_height="1dp"
          android:background="#E0E0E0"
          android:layout_below="@id/llToolbar" />

      <!-- 2. Horizontal Tabs -->
      <LinearLayout
          android:id="@+id/llTabs"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:orientation="horizontal"
          android:background="#FFFFFF"
          android:layout_below="@id/divider1">

          <!-- Tab Sản Phẩm (Active) -->
          <LinearLayout
              android:id="@+id/tabProducts"
              android:layout_width="0dp"
              android:layout_height="wrap_content"
              android:layout_weight="1"
              android:orientation="vertical"
              android:gravity="center"
              android:paddingVertical="10dp"
              android:clickable="true"
              android:focusable="true"
              android:background="?attr/selectableItemBackground">
              <TextView
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:text="Sản phẩm"
                  android:textColor="@color/colorTabActive"
                  android:textStyle="bold"
                  android:textSize="13sp" />
              <View
                  android:layout_width="40dp"
                  android:layout_height="3dp"
                  android:background="@color/colorTabActive"
                  android:layout_marginTop="4dp"/>
          </LinearLayout>

          <!-- Tab Tồn Kho -->
          <LinearLayout
              android:id="@+id/tabInventory"
              android:layout_width="0dp"
              android:layout_height="wrap_content"
              android:layout_weight="1"
              android:orientation="vertical"
              android:gravity="center"
              android:paddingVertical="10dp"
              android:clickable="true"
              android:focusable="true"
              android:background="?attr/selectableItemBackground">
              <TextView
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:text="Tồn kho"
                  android:textColor="#757575"
                  android:textSize="13sp" />
              <View
                  android:layout_width="40dp"
                  android:layout_height="3dp"
                  android:background="@android:color/transparent"
                  android:layout_marginTop="4dp"/>
          </LinearLayout>

          <!-- Tab Bán Kèm -->
          <LinearLayout
              android:id="@+id/tabBundle"
              android:layout_width="0dp"
              android:layout_height="wrap_content"
              android:layout_weight="1"
              android:orientation="vertical"
              android:gravity="center"
              android:paddingVertical="10dp"
              android:clickable="true"
              android:focusable="true"
              android:background="?attr/selectableItemBackground">
              <TextView
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:text="Bán kèm"
                  android:textColor="#757575"
                  android:textSize="13sp" />
              <View
                  android:layout_width="40dp"
                  android:layout_height="3dp"
                  android:background="@android:color/transparent"
                  android:layout_marginTop="4dp"/>
          </LinearLayout>

          <!-- Tab Danh Mục -->
          <LinearLayout
              android:id="@+id/tabCategory"
              android:layout_width="0dp"
              android:layout_height="wrap_content"
              android:layout_weight="1"
              android:orientation="vertical"
              android:gravity="center"
              android:paddingVertical="10dp"
              android:clickable="true"
              android:focusable="true"
              android:background="?attr/selectableItemBackground">
              <TextView
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:text="Danh mục"
                  android:textColor="#757575"
                  android:textSize="13sp" />
              <View
                  android:layout_width="40dp"
                  android:layout_height="3dp"
                  android:background="@android:color/transparent"
                  android:layout_marginTop="4dp"/>
          </LinearLayout>
      </LinearLayout>

      <View
          android:id="@+id/divider2"
          android:layout_width="match_parent"
          android:layout_height="1dp"
          android:background="#E0E0E0"
          android:layout_below="@id/llTabs" />

      <!-- 3. Horizontal Pill Filters -->
      <LinearLayout
          android:id="@+id/llFilters"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:orientation="horizontal"
          android:gravity="center_vertical"
          android:background="#FFFFFF"
          android:paddingVertical="8dp"
          android:paddingHorizontal="12dp"
          android:layout_below="@id/divider2">

          <HorizontalScrollView
              android:layout_width="0dp"
              android:layout_height="wrap_content"
              android:layout_weight="1"
              android:scrollbars="none">

              <LinearLayout
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:orientation="horizontal"
                  android:gap="8dp">

                  <!-- Pill: Tất cả -->
                  <TextView
                      android:id="@+id/pillAll"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"
                      android:text="Tất cả"
                      android:textColor="@color/colorPillActiveBorder"
                      android:background="@drawable/bg_pill_active"
                      android:paddingHorizontal="14dp"
                      android:paddingVertical="6dp"
                      android:textSize="12sp"
                      android:clickable="true"
                      android:focusable="true" />

                  <!-- Pill: Đồ máy -->
                  <TextView
                      android:id="@+id/pillMachine"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"
                      android:layout_marginStart="8dp"
                      android:text="Đồ máy"
                      android:textColor="#5F6368"
                      android:background="@drawable/bg_pill_inactive"
                      android:paddingHorizontal="14dp"
                      android:paddingVertical="6dp"
                      android:textSize="12sp"
                      android:clickable="true"
                      android:focusable="true" />

                  <!-- Pill: Phụ kiện sửa chữa -->
                  <TextView
                      android:id="@+id/pillAccessories"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"
                      android:layout_marginStart="8dp"
                      android:text="Phụ kiện sửa chữa"
                      android:textColor="#5F6368"
                      android:background="@drawable/bg_pill_inactive"
                      android:paddingHorizontal="14dp"
                      android:paddingVertical="6dp"
                      android:textSize="12sp"
                      android:clickable="true"
                      android:focusable="true" />
              </LinearLayout>
          </HorizontalScrollView>

          <ImageView
              android:id="@+id/btnPillsGrid"
              android:layout_width="28dp"
              android:layout_height="28dp"
              android:layout_marginStart="12dp"
              android:src="@android:drawable/ic_dialog_dialer"
              app:tint="#1A73E8"
              android:contentDescription="Lưới"
              android:clickable="true"
              android:focusable="true" />
      </LinearLayout>

      <!-- 4. Product Recycler View -->
      <androidx.recyclerview.widget.RecyclerView
          android:id="@+id/rvProducts"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:layout_below="@id/llFilters"
          android:clipToPadding="false"
          android:paddingHorizontal="12dp"
          android:paddingVertical="4dp" />

      <!-- 5. Floating Action Button -->
      <com.google.android.material.floatingactionbutton.FloatingActionButton
          android:id="@+id/fabAdd"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:layout_alignParentBottom="true"
          android:layout_alignParentEnd="true"
          android:layout_margin="16dp"
          android:src="@android:drawable/ic_input_add"
          app:backgroundTint="@color/colorAccentBlue"
          app:tint="#FFFFFF"
          app:elevation="6dp"
          android:contentDescription="Thêm sản phẩm" />

  </RelativeLayout>
  ```

- [ ] **Step 2: Commit**
  ```bash
  git add app/src/main/res/layout/fragment_home.xml
  git commit -m "feat: redesign home screen layout with premium toolbar, tabs and filters"
  ```

---

### Task 3: Redesign layout thẻ sản phẩm `item_product.xml`
Cập nhật thiết kế các thẻ sản phẩm trong danh sách với cấu trúc gọn gàng, có đơn vị tính mặc định, giá màu cam đậm và nút chia sẻ bên phải.

**Files:**
- Modify: `app/src/main/res/layout/item_product.xml`

- [ ] **Step 1: Thay thế toàn bộ code `item_product.xml`**
  ```xml
  <!-- File: app/src/main/res/layout/item_product.xml -->
  <?xml version="1.0" encoding="utf-8"?>
  <com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:app="http://schemas.android.com/apk/res-auto"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_marginHorizontal="2dp"
      android:layout_marginVertical="5dp"
      app:cardCornerRadius="8dp"
      app:cardElevation="1dp"
      app:strokeWidth="1dp"
      app:strokeColor="#E0E0E0"
      app:cardBackgroundColor="#FFFFFF">

      <androidx.constraintlayout.widget.ConstraintLayout
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:padding="10dp">

          <!-- Rounded Image container on Left -->
          <com.google.android.material.card.MaterialCardView
              android:id="@+id/cardImage"
              android:layout_width="60dp"
              android:layout_height="60dp"
              app:cardCornerRadius="8dp"
              app:cardElevation="0dp"
              app:strokeWidth="1dp"
              app:strokeColor="#E0E0E0"
              app:layout_constraintStart_toStartOf="parent"
              app:layout_constraintTop_toTopOf="parent"
              app:layout_constraintBottom_toBottomOf="parent">

              <ImageView
                  android:id="@+id/imgProduct"
                  android:layout_width="match_parent"
                  android:layout_height="match_parent"
                  android:scaleType="centerCrop"
                  android:src="@android:drawable/ic_menu_gallery"
                  android:contentDescription="Ảnh sản phẩm" />
          </com.google.android.material.card.MaterialCardView>

          <!-- Product Name & Unit & Price Stack -->
          <LinearLayout
              android:layout_width="0dp"
              android:layout_height="wrap_content"
              android:orientation="vertical"
              android:layout_marginStart="12dp"
              android:layout_marginEnd="12dp"
              app:layout_constraintStart_toEndOf="@id/cardImage"
              app:layout_constraintEnd_toStartOf="@id/btnShare"
              app:layout_constraintTop_toTopOf="parent"
              app:layout_constraintBottom_toBottomOf="parent">

              <TextView
                  android:id="@+id/tvProductName"
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:textColor="@color/colorTextDark"
                  android:textSize="13.5sp"
                  android:textStyle="bold"
                  android:maxLines="1"
                  android:ellipsize="end" />

              <TextView
                  android:id="@+id/tvProductUnit"
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:layout_marginTop="2dp"
                  android:text="Bộ"
                  android:textColor="@color/colorTextGrey"
                  android:textSize="11sp" />

              <TextView
                  android:id="@+id/tvSellPriceNew"
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:layout_marginTop="4dp"
                  android:textColor="@color/colorPriceOrange"
                  android:textSize="13sp"
                  android:textStyle="bold" />
          </LinearLayout>

          <!-- Rounded Share Button on Right -->
          <FrameLayout
              android:id="@+id/btnShare"
              android:layout_width="32dp"
              android:layout_height="32dp"
              android:background="@drawable/bg_share_btn"
              android:clickable="true"
              android:focusable="true"
              app:layout_constraintEnd_toEndOf="parent"
              app:layout_constraintTop_toTopOf="parent"
              app:layout_constraintBottom_toBottomOf="parent">

              <ImageView
                  android:layout_width="16dp"
                  android:layout_height="16dp"
                  android:layout_gravity="center"
                  android:src="@drawable/ic_share"
                  app:tint="#1A73E8"
                  android:contentDescription="Chia sẻ" />
          </FrameLayout>

      </androidx.constraintlayout.widget.ConstraintLayout>
  </com.google.android.material.card.MaterialCardView>
  ```

- [ ] **Step 2: Commit**
  ```bash
  git add app/src/main/res/layout/item_product.xml
  git commit -m "feat: upgrade item product design with modern card aesthetics and share button"
  ```

---

### Task 4: Chuyển Đăng xuất/Lời chào sang tab "Cài đặt" (More tab)
Thiết kế giao diện tab cài đặt đẹp mắt chứa thông tin tài khoản và nút Đăng xuất.

**Files:**
- Modify: `app/src/main/res/layout/fragment_more.xml`
- Modify: `app/src/main/java/com/example/myapplication/fragment/MoreFragment.java`

- [ ] **Step 1: Viết lại `fragment_more.xml`**
  ```xml
  <!-- File: app/src/main/res/layout/fragment_more.xml -->
  <?xml version="1.0" encoding="utf-8"?>
  <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:app="http://schemas.android.com/apk/res-auto"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:orientation="vertical"
      android:background="#F5F5F5"
      android:padding="16dp">

      <TextView
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:text="Cài đặt hệ thống"
          android:textSize="20sp"
          android:textStyle="bold"
          android:textColor="#212121"
          android:layout_marginBottom="16dp" />

      <!-- Profile Information Card -->
      <com.google.android.material.card.MaterialCardView
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          app:cardCornerRadius="12dp"
          app:cardElevation="2dp"
          app:strokeWidth="0dp"
          app:cardBackgroundColor="#FFFFFF"
          android:layout_marginBottom="20dp">

          <LinearLayout
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:orientation="horizontal"
              android:gravity="center_vertical"
              android:padding="16dp">

              <ImageView
                  android:layout_width="54dp"
                  android:layout_height="54dp"
                  android:src="@android:drawable/ic_menu_myplaces"
                  app:tint="@color/colorTabActive" />

              <LinearLayout
                  android:layout_width="0dp"
                  android:layout_height="wrap_content"
                  android:layout_weight="1"
                  android:orientation="vertical"
                  android:layout_marginStart="16dp">

                  <TextView
                      android:id="@+id/tvGreeting"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"
                      android:text="Chào bạn 👋"
                      android:textColor="#212121"
                      android:textSize="15sp"
                      android:textStyle="bold" />

                  <TextView
                      android:id="@+id/tvEmail"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"
                      android:layout_marginTop="2dp"
                      android:textColor="#757575"
                      android:textSize="12sp" />
              </LinearLayout>
          </LinearLayout>
      </com.google.android.material.card.MaterialCardView>

      <!-- Logout Button Container -->
      <Button
          android:id="@+id/btnLogout"
          android:layout_width="match_parent"
          android:layout_height="48dp"
          android:backgroundTint="#FF3B30"
          android:text="ĐĂNG XUẤT TÀI KHOẢN"
          android:textColor="#FFFFFF"
          android:textStyle="bold"
          android:textSize="13sp"
          app:cornerRadius="8dp" />

  </LinearLayout>
  ```

- [ ] **Step 2: Cập nhật code Java trong `MoreFragment.java`**
  ```java
  // File: app/src/main/java/com/example/myapplication/fragment/MoreFragment.java
  package com.example.myapplication.fragment;

  import android.content.Intent;
  import android.os.Bundle;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import android.widget.Button;
  import android.widget.TextView;

  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.fragment.app.Fragment;

  import com.example.myapplication.LoginActivity;
  import com.example.myapplication.R;
  import com.example.myapplication.helpers.PreferencesHelper;
  import com.google.firebase.auth.FirebaseAuth;
  import com.google.firebase.auth.FirebaseUser;

  public class MoreFragment extends Fragment {

      @Nullable
      @Override
      public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
          View view = inflater.inflate(R.layout.fragment_more, container, false);

          TextView tvGreeting = view.findViewById(R.id.tvGreeting);
          TextView tvEmail = view.findViewById(R.id.tvEmail);
          Button btnLogout = view.findViewById(R.id.btnLogout);

          FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
          if (user != null && user.getEmail() != null) {
              String name = user.getEmail().split("@")[0];
              tvGreeting.setText("Chào " + name + " 👋");
              tvEmail.setText(user.getEmail());
          }

          btnLogout.setOnClickListener(v -> {
              FirebaseAuth.getInstance().signOut();
              new PreferencesHelper(requireContext()).logout();
              startActivity(new Intent(getActivity(), LoginActivity.class));
              if (getActivity() != null) getActivity().finish();
          });

          return view;
      }
  }
  ```

- [ ] **Step 3: Commit**
  ```bash
  git add app/src/main/res/layout/fragment_more.xml app/src/main/java/com/example/myapplication/fragment/MoreFragment.java
  git commit -m "feat: move user profile greeting and logout button to settings tab"
  ```

---

### Task 5: Cập nhật Adapter hiển thị sản phẩm `ProductAdapter.java`
Thay đổi binding views trong ViewHolder của ProductAdapter để khớp với các view mới của `item_product.xml`, cài đặt mặc định đơn vị tính "Bộ" và sự kiện chia sẻ.

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/adapters/ProductAdapter.java`

- [ ] **Step 1: Sửa đổi class `ProductAdapter.java`**
  ```java
  // Thay thế file: app/src/main/java/com/example/myapplication/adapters/ProductAdapter.java
  package com.example.myapplication.adapters;

  import android.content.Context;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import android.widget.ImageView;
  import android.widget.TextView;
  import android.widget.Toast;

  import androidx.annotation.NonNull;
  import androidx.recyclerview.widget.RecyclerView;

  import com.bumptech.glide.Glide;
  import com.example.myapplication.R;
  import com.example.myapplication.models.Product;

  import java.util.ArrayList;
  import java.util.List;
  import java.util.Locale;

  public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

      private final Context context;
      private List<Product> productList = new ArrayList<>();
      private final OnProductClickListener listener;

      public interface OnProductClickListener {
          void onProductClick(Product product);
      }

      public ProductAdapter(Context context, OnProductClickListener listener) {
          this.context = context;
          this.listener = listener;
      }

      public void setProducts(List<Product> products) {
          this.productList = products;
          notifyDataSetChanged();
      }

      @NonNull
      @Override
      public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
          View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
          return new ViewHolder(view);
      }

      @Override
      public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
          Product product = productList.get(position);

          holder.tvProductName.setText(product.getProduct_name());
          holder.tvSellPriceNew.setText(formatCurrency(product.getSell_price()));

          // Mặc định Đơn vị tính hiển thị là "Bộ"
          holder.tvProductUnit.setText("Bộ");

          // Load image using Glide
          if (product.getImage_url() != null && !product.getImage_url().isEmpty()) {
              holder.imgProduct.setImageTintList(null);
              
              String finalUrl = product.getImage_url();
              if (finalUrl.contains("/object/public/")) {
                  finalUrl = finalUrl.replace("/object/public/", "/object/");
              }

              com.bumptech.glide.load.model.GlideUrl glideUrl = new com.bumptech.glide.load.model.GlideUrl(finalUrl,
                  new com.bumptech.glide.load.model.LazyHeaders.Builder()
                      .addHeader("Authorization", "Bearer " + com.example.myapplication.BuildConfig.SUPABASE_ANON_KEY)
                      .build());
              
              Glide.with(context)
                      .load(glideUrl)
                      .placeholder(android.R.drawable.ic_menu_gallery)
                      .into(holder.imgProduct);
          } else {
              holder.imgProduct.setImageTintList(android.content.res.ColorStateList.valueOf(
                      androidx.core.content.ContextCompat.getColor(context, android.R.color.darker_gray)));
              holder.imgProduct.setImageResource(android.R.drawable.ic_menu_gallery);
          }

          // Handle click on complete item row
          holder.itemView.setOnClickListener(v -> {
              if (listener != null) {
                  listener.onProductClick(product);
              }
          });

          // Handle click on Share Button
          holder.btnShare.setOnClickListener(v -> {
              Toast.makeText(context, "Đang chuẩn bị chia sẻ sản phẩm " + product.getProduct_name() + "...", Toast.LENGTH_SHORT).show();
          });
      }

      @Override
      public int getItemCount() {
          return productList.size();
      }

      private String formatCurrency(Double amount) {
          if (amount == null) return "0 đ";
          return String.format(Locale.getDefault(), "%,.0f đ", amount);
      }

      static class ViewHolder extends RecyclerView.ViewHolder {
          ImageView imgProduct;
          TextView tvProductName, tvProductUnit, tvSellPriceNew;
          View btnShare;

          public ViewHolder(@NonNull View itemView) {
              super(itemView);
              imgProduct = itemView.findViewById(R.id.imgProduct);
              tvProductName = itemView.findViewById(R.id.tvProductName);
              tvProductUnit = itemView.findViewById(R.id.tvProductUnit);
              tvSellPriceNew = itemView.findViewById(R.id.tvSellPriceNew);
              btnShare = itemView.findViewById(R.id.btnShare);
          }
      }
  }
  ```

- [ ] **Step 2: Commit**
  ```bash
  git add app/src/main/java/com/example/myapplication/adapters/ProductAdapter.java
  git commit -m "refactor: update ProductAdapter to match the new item_product.xml and support share click"
  ```

---

### Task 6: Cập nhật Home controller `HomeFragment.java`
Liên kết các sự kiện cho Toolbar tìm kiếm mới, Back button, Tabs nằm ngang, các Pills nhãn lọc, và Floating Action Button.

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/fragment/HomeFragment.java`

- [ ] **Step 1: Viết lại logic `HomeFragment.java`**
  ```java
  // Thay thế file: app/src/main/java/com/example/myapplication/fragment/HomeFragment.java
  package com.example.myapplication.fragment;

  import android.content.Context;
  import android.content.Intent;
  import android.os.Bundle;
  import android.os.Handler;
  import android.os.Looper;
  import android.text.Editable;
  import android.text.TextWatcher;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import android.view.inputmethod.InputMethodManager;
  import android.widget.EditText;
  import android.widget.ImageButton;
  import android.widget.TextView;
  import android.widget.Toast;

  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.fragment.app.Fragment;
  import androidx.recyclerview.widget.LinearLayoutManager;
  import androidx.recyclerview.widget.RecyclerView;

  import com.example.myapplication.ProductDetailActivity;
  import com.example.myapplication.R;
  import com.example.myapplication.adapters.ProductAdapter;
  import com.example.myapplication.helpers.FirebaseHelper;
  import com.example.myapplication.models.Product;
  import com.google.android.material.floatingactionbutton.FloatingActionButton;
  import com.google.firebase.firestore.QueryDocumentSnapshot;

  import java.util.ArrayList;
  import java.util.Collections;
  import java.util.List;

  public class HomeFragment extends Fragment {

      private ProductAdapter adapter;
      private EditText etSearch;
      private FirebaseHelper firebaseHelper;
      private final List<Product> allProducts = new ArrayList<>();
      private final Handler debounceHandler = new Handler(Looper.getMainLooper());
      private Runnable searchRunnable;

      @Nullable
      @Override
      public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
          View view = inflater.inflate(R.layout.fragment_home, container, false);

          firebaseHelper = new FirebaseHelper();

          // 1. Ánh xạ các View chính
          RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
          etSearch = view.findViewById(R.id.etSearch);
          FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);

          ImageButton btnBack = view.findViewById(R.id.btnBack);
          ImageButton btnScan = view.findViewById(R.id.btnScan);
          ImageButton btnSort = view.findViewById(R.id.btnSort);
          ImageButton btnGridToggle = view.findViewById(R.id.btnGridToggle);

          // 2. Ánh xạ các View phụ (Tabs & Pills)
          View tabProducts = view.findViewById(R.id.tabProducts);
          View tabInventory = view.findViewById(R.id.tabInventory);
          View tabBundle = view.findViewById(R.id.tabBundle);
          View tabCategory = view.findViewById(R.id.tabCategory);

          View pillAll = view.findViewById(R.id.pillAll);
          View pillMachine = view.findViewById(R.id.pillMachine);
          View pillAccessories = view.findViewById(R.id.pillAccessories);
          View btnPillsGrid = view.findViewById(R.id.btnPillsGrid);

          // 3. Thiết lập RecyclerView & Adapter
          rvProducts.setLayoutManager(new LinearLayoutManager(getContext()));
          adapter = new ProductAdapter(getContext(), product -> {
              Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
              intent.putExtra("PRODUCT_ID", product.getId());
              startActivity(intent);
          });
          rvProducts.setAdapter(adapter);

          // 4. Thiết lập sự kiện cho các Nút Toolbar Tìm Kiếm
          btnBack.setOnClickListener(v -> {
              if (etSearch.getText().length() > 0) {
                  etSearch.setText("");
              }
              hideKeyboard();
          });

          View.OnClickListener staticFeatureListener = v -> 
              Toast.makeText(getContext(), "Tính năng đang được phát triển!", Toast.LENGTH_SHORT).show();

          btnScan.setOnClickListener(staticFeatureListener);
          btnSort.setOnClickListener(staticFeatureListener);
          btnGridToggle.setOnClickListener(staticFeatureListener);

          // 5. Thiết lập sự kiện cho các Tab ngang
          tabProducts.setOnClickListener(v -> {
              // Tab hiện tại đang chọn
          });
          tabInventory.setOnClickListener(staticFeatureListener);
          tabBundle.setOnClickListener(staticFeatureListener);
          tabCategory.setOnClickListener(staticFeatureListener);

          // 6. Thiết lập sự kiện cho các Pills nhãn lọc
          pillAll.setOnClickListener(v -> {
              // Nhãn Tất cả đang được chọn
          });
          pillMachine.setOnClickListener(staticFeatureListener);
          pillAccessories.setOnClickListener(staticFeatureListener);
          btnPillsGrid.setOnClickListener(staticFeatureListener);

          // 7. Thiết lập sự kiện nút FAB
          fabAdd.setOnClickListener(v -> {
              startActivity(new Intent(getActivity(), ProductDetailActivity.class));
          });

          // 8. Tải dữ liệu & Debounce Tìm kiếm
          setupSearchDebounce();
          loadProducts();

          return view;
      }

      private void loadProducts() {
          firebaseHelper.listenForProducts((value, error) -> {
              if (error != null) {
                  if (getContext() != null) {
                      Toast.makeText(getContext(), "Lỗi Firestore: " + error.getMessage(), Toast.LENGTH_LONG).show();
                  }
                  return;
              }
              if (value == null) return;

              allProducts.clear();
              for (QueryDocumentSnapshot doc : value) {
                  Product p = doc.toObject(Product.class);
                  p.setId(doc.getId());
                  allProducts.add(p);
              }

              // Sắp xếp theo ngày cập nhật mới nhất
              Collections.sort(allProducts, (p1, p2) -> {
                  Long t1 = p1.getUpdated_at() != null ? p1.getUpdated_at() : 0L;
                  Long t2 = p2.getUpdated_at() != null ? p2.getUpdated_at() : 0L;
                  return t2.compareTo(t1);
              });

              filterProducts(etSearch.getText().toString());
          });
      }

      private void setupSearchDebounce() {
          etSearch.addTextChangedListener(new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

              @Override
              public void onTextChanged(CharSequence s, int start, int before, int count) {
                  if (searchRunnable != null) {
                      debounceHandler.removeCallbacks(searchRunnable);
                  }
              }

              @Override
              public void afterTextChanged(Editable s) {
                  searchRunnable = () -> filterProducts(s.toString());
                  debounceHandler.postDelayed(searchRunnable, 300);
              }
          });
      }

      private void filterProducts(String query) {
          if (query.trim().isEmpty()) {
              adapter.setProducts(new ArrayList<>(allProducts));
              return;
          }

          String lowerQuery = query.toLowerCase().trim();
          List<Product> filtered = new ArrayList<>();
          for (Product p : allProducts) {
              String name = p.getProduct_name() != null ? p.getProduct_name().toLowerCase() : "";
              String code = p.getProduct_code() != null ? p.getProduct_code().toLowerCase() : "";

              if (name.contains(lowerQuery) || code.contains(lowerQuery)) {
                  filtered.add(p);
              }
          }
          adapter.setProducts(filtered);
      }

      private void hideKeyboard() {
          if (getActivity() != null && getView() != null) {
              InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
              if (imm != null) {
                  imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
              }
          }
      }
  }
  ```

- [ ] **Step 2: Commit**
  ```bash
  git add app/src/main/java/com/example/myapplication/fragment/HomeFragment.java
  git commit -m "feat: complete HomeFragment controller update to match premium search UI actions"
  ```

---

### Task 7: Biên dịch dự án & Xác nhận tính chính xác
Chạy tác vụ gradle để biên dịch toàn bộ dự án Android và đảm bảo không có lỗi biên dịch nào liên quan tới các Views, IDs hay Imports mới.

**Files:**
- Modify: Không có.
- Test: Build project.

- [ ] **Step 1: Biên dịch toàn bộ dự án**
  Chạy lệnh gradle build:
  Run: `./gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Xác nhận hoàn thành**
  Hoàn thành việc nâng cấp giao diện, báo cáo lại kết quả với người dùng.
