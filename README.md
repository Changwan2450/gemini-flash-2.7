# 🚀 Member Full-Stack Service

## Tech Stack
| 기술        | 아이콘                 |
|-------------|-----------------------|
| Java        | ![Java](https://img.icons8.com/color/48/000000/java-coffee-cup-logo.png) |
| Spring      | ![Spring](https://img.icons8.com/color/48/000000/spring-logo.png) |
| MyBatis     | ![MyBatis](https://img.icons8.com/color/48/000000/mybatis.png) |
| Oracle      | ![Oracle](https://img.icons8.com/color/48/000000/oracle-logo.png) |
| JSP         | ![JSP](https://img.icons8.com/color/48/000000/java-server-pages.png) |

## Controller 역할 구분

| 구분           | 일반 Controller              | RestController                |
|----------------|-------------------------------|-------------------------------|
| 반환 타입      | JSP 페이지                    | JSON 데이터                   |
| 사용 용도      | 웹 페이지 포워딩             | API 응답                      |
| 예시 메서드    | `getMemberPage()`            | `getMemberData()`             |

## 프로젝트 구조
```
./src
├── main
│   ├── java
│   │   └── com
│   │       └── kh
│   │           └── geminibot
│   └── resources
├── test
│   └── java
└── ...
``` 

## 설정 파일
- **pom.xml**: Maven 프로젝트 설정 및 의존성 관리.
- **application.properties**: 애플리케이션 설정.
