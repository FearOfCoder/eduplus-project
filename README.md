# DoAn10Diem - Spring Boot Web (Java 21)

Dự án web mẫu dùng **Spring Boot** và **Maven**, được cấu hình sẵn cho **Java 21**.

## Cấu trúc chính

- `pom.xml` - cấu hình Maven & Spring Boot.
- `src/main/java/com/example/doan10diem` - mã nguồn Java:
  - `DoAn10DiemApplication` - lớp main, entry point.
  - `controller/HomeController` - controller trả về trang chủ.
- `src/main/resources/templates/index.html` - trang HTML sử dụng Thymeleaf.
- `src/main/resources/static/css/styles.css` - CSS tùy biến giao diện.
- `src/test/java/com/example/doan10diem` - test cơ bản.

## Cách chạy

Yêu cầu:

- Java 21 đã cài sẵn (đã có trên máy bạn).
- Maven 3.9.x (đã có trên máy bạn).

Trong thư mục dự án `d:\DoAn10diem`, mở terminal và chạy:

```bash
mvn spring-boot:run
```

Sau khi build và chạy xong, mở trình duyệt truy cập:

- `http://localhost:8080`

Bạn sẽ thấy trang chủ "Đồ án 10 điểm" với giao diện hiện đại (Bootstrap).

## Build file jar

```bash
mvn clean package
```

File jar sẽ nằm trong thư mục:

- `target/doan10diem-0.0.1-SNAPSHOT.jar`

Chạy jar:

```bash
java -jar target/doan10diem-0.0.1-SNAPSHOT.jar
```

