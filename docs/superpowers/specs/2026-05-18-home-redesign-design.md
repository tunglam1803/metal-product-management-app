# Tài liệu Thiết kế: Nâng Cấp Toàn Diện Giao Diện Danh Sách Sản Phẩm (Home Screen Redesign)

**Ngày:** 2026-05-18  
**Trạng thái:** Chờ Duyệt (Draft)  
**Tác giả:** Antigravity (AI Coding Assistant)  
**Quyết định chính:** 
- Thiết kế lại Toolbar trên cùng, Tab điều hướng nằm ngang, nhãn lọc scroll ngang, và thẻ sản phẩm theo chuẩn hiện đại từ ảnh chụp màn hình.
- Nút đăng xuất và Lời chào sẽ được đưa sang tab "Cài đặt" (MoreFragment).
- Các nút quét mã, bộ lọc nâng cao, và chuyển đổi lưới/dạng danh sách được làm giao diện tĩnh (Static) hiển thị Toast "Tính năng đang được phát triển" khi click, tập trung tối ưu hóa tìm kiếm sản phẩm theo Tên/Mã SKU.

---

## 1. Thay đổi tài nguyên & Màu sắc (`colors.xml`)

Để hỗ trợ giao diện mới với tông màu xanh lục đậm làm chủ đạo cho tab, xanh lam cho nút FAB và cam đậm cho giá tiền, chúng ta sẽ thêm/cập nhật các màu sắc sau trong `colors.xml`:

```xml
<!-- Thêm vào res/values/colors.xml -->
<color name="colorAccentBlue">#2979FF</color>       <!-- Màu xanh nút FAB -->
<color name="colorPriceOrange">#E65100</color>     <!-- Màu cam đậm của giá bán -->
<color name="colorTabActive">#2E7D32</color>       <!-- Màu xanh lá cho tab hoạt động -->
<color name="colorPillActiveBorder">#1A73E8</color> <!-- Màu viền xanh lam cho nhãn lọc active -->
<color name="colorPillInactiveBg">#F1F3F4</color>   <!-- Nền xám nhạt cho nhãn lọc chưa active -->
<color name="colorPillActiveBg">#FFFFFF</color>     <!-- Nền trắng cho nhãn lọc active -->
<color name="colorTextDark">#202124</color>         <!-- Chữ chính đậm -->
<color name="colorTextGrey">#70757A</color>         <!-- Chữ phụ nhạt -->
<color name="colorShareBorder">#DADCE0</color>      <!-- Viền nút chia sẻ -->
```

---

## 2. Giao diện Màn hình chính (`fragment_home.xml`)

Chúng ta thay thế toàn bộ layout cũ của `fragment_home.xml` bằng một cấu trúc gồm:
1. **Search Bar Toolbar**:
   - Nút Back (`<-`) làm bằng `ImageButton`. Khi click sẽ xoá ô tìm kiếm và ẩn bàn phím.
   - Khung tìm kiếm bo tròn với Icon kính lúp (`🔍`), ô nhập `EditText` (`Tìm tên, mã SKU, ...`) và nút quét mã vạch ở trong khung.
   - Nút Sắp xếp (`⇄`) làm bằng `ImageButton`.
   - Nút Đổi dạng hiển thị/Lưới (`㗊`) làm bằng `ImageButton`.
2. **Thanh Tab điều hướng ngang**:
   - Gồm 4 tab tĩnh: "Sản phẩm", "Tồn kho", "Bán kèm", "Danh mục" xếp hàng ngang.
   - Tab "Sản phẩm" được chọn mặc định và có gạch chân màu xanh lá cây (`#2E7D32`).
3. **Thanh Nhãn Lọc (Horizontal Pill Filters)**:
   - Một `HorizontalScrollView` chứa các `TextView` dạng viên thuốc (Pill): "Tất cả", "Đồ máy", "Phụ kiện sửa chữa", v.v.
   - Một icon lưới `㗊` nằm cố định ở phía bên phải.
4. **RecyclerView (`rvProducts`)**:
   - Danh sách sản phẩm hiển thị chiếm trọn phần không gian còn lại.
5. **Floating Action Button (`fabAdd`)**:
   - Nút tròn màu xanh lam (`#2979FF`) với biểu tượng dấu `+` màu trắng, nằm ở góc dưới cùng bên phải.

---

## 3. Giao diện Thẻ Sản Phẩm (`item_product.xml`)

Chúng ta nâng cấp giao diện từng hàng sản phẩm trong danh sách:
- **Ảnh sản phẩm**: Được đặt trong `MaterialCardView` có `cardCornerRadius="8dp"` và `strokeWidth="1dp"` màu viền xám nhạt, tạo góc bo tròn tinh tế hơn.
- **Tên sản phẩm**: Sử dụng font chữ đậm hơn (`fontFamily="sans-serif-medium"`, `textSize="14sp"`, `textColor="@color/colorTextDark"`).
- **Đơn vị tính**: Hiển thị chữ `"Bộ"` màu xám ngay bên dưới tên sản phẩm.
- **Giá bán sản phẩm**: Được đưa xuống dưới cùng, hiển thị cam đậm nổi bật (`textColor="@color/colorPriceOrange"`, `textSize="14sp"`, `textStyle="bold"`).
- **Nút chia sẻ (`prop-item-share`)**: Được thiết kế là một `FrameLayout` bo tròn viền xám ở góc phải ngoài cùng của thẻ, bên trong có hình icon Share màu xanh lam.

---

## 4. Giao diện Tab "Cài đặt" mới (`fragment_more.xml`)

Chúng ta di chuyển nút đăng xuất và thông tin người dùng sang màn hình này:
- Một Card thông tin tài khoản hiển thị Avatar mặc định và Email tài khoản đang đăng nhập.
- Nút "Đăng xuất" dạng nút viền đỏ chữ đỏ hoặc nút bấm nổi bật, kèm theo icon đăng xuất để tạo cảm giác an toàn và chuyên nghiệp.

---

## 5. Logic xử lý Java

### A. Cập nhật `HomeFragment.java`
- Ánh xạ lại các View mới của Toolbar tìm kiếm, các nút bấm phụ.
- Thêm sự kiện click cho các nút tĩnh (`⇄`, quét mã, `㗊`, các Tab nằm ngang, các nhãn lọc viên thuốc): khi bấm sẽ hiển thị `Toast` *"Tính năng đang được phát triển"*.
- Nút Back (`<-`): nếu ô tìm kiếm đang có chữ, bấm vào sẽ xoá trống ô tìm kiếm và ẩn bàn phím.
- Giữ nguyên logic Firebase lắng nghe dữ liệu thời gian thực và debounce tìm kiếm theo tên hoặc mã sản phẩm.

### B. Cập nhật `ProductAdapter.java`
- Cập nhật ViewHolder để liên kết với các thuộc tính giao diện mới (tên, ảnh, giá bán màu cam, nút chia sẻ).
- Đơn vị tính: Hiển thị mặc định là `"Bộ"` cho tất cả các sản phẩm.
- Nút chia sẻ: Khi bấm vào sẽ hiển thị `Toast` *"Đang chuẩn bị chia sẻ sản phẩm [Tên sản phẩm]..."*.

### C. Cập nhật `MoreFragment.java` & `fragment_more.xml`
- Đọc thông tin email của tài khoản FirebaseAuth đang đăng nhập để hiển thị lên giao diện.
- Bắt sự kiện nút Đăng xuất, thực hiện lệnh `FirebaseAuth.getInstance().signOut()`, xoá preferences và điều hướng về màn hình `LoginActivity`.

---

## 6. Kế hoạch kiểm thử (Testing Plan)
1. **Kiểm tra giao diện (Visual Verification)**: Mở ứng dụng, kiểm tra xem Toolbar tìm kiếm, thanh tab ngang, thanh nhãn lọc và danh sách sản phẩm có hiển thị chính xác theo ảnh chụp màn hình hay không.
2. **Tìm kiếm (Search Functionality)**: Nhập tên sản phẩm hoặc mã SKU và kiểm tra xem danh sách có tự động lọc đúng sản phẩm mong muốn sau khoảng trễ debounce (300ms) hay không.
3. **Các nút phụ (Helper Buttons)**: Click các nút quét mã, sắp xếp, đổi giao diện, tab "Tồn kho", tab "Bán kèm", các pill lọc khác và kiểm tra xem có hiện Toast thông báo tương ứng hay không.
4. **Đăng xuất (Logout Flow)**: Bấm tab "Cài đặt", kiểm tra xem email hiển thị có đúng không, bấm "Đăng xuất" xem ứng dụng có đóng phiên làm việc và chuyển về màn hình đăng nhập hay không.
