## 프로젝트 구조
```
com.modu.backend
├── domain
│   ├── user
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   │       ├── request
│   │       └── response
│   │
│   ├── attraction
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   │       ├── request
│   │       └── response
│   │
│   ├── accessibility
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   ├── enums
│   │   ├── parser
│   │   └── dto
│   │       ├── request
│   │       └── response
│   │
│   └── schedule
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
│           ├── request
│           └── response
│
├── external
│   ├── tourapi
│   │   ├── client
│   │   ├── dto
│   │   │   ├── request
│   │   │   └── response
│   │   ├── mapper
│   │   └── properties
│   │
│   └── kakao
│       ├── client
│       ├── dto
│       │   ├── request
│       │   └── response
│       ├── mapper
│       └── properties
│
├── batch
│   └── tourapi
│       ├── service
│       └── scheduler
│
└── global
    ├── config
    ├── exception
    ├── response
    ├── security
    └── util
```
