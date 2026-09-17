# AI个性化膳食规划与饮食健康管理系统 (DietAI-Planner)

## 项目架构

```
DietAI-Planner/
├── src/main/java/com/zyyqq/     # Spring Boot 后端
│   ├── entity/                    # JPA实体类 (8张表)
│   ├── repository/                # 数据访问层
│   ├── service/                   # 业务逻辑层
│   ├── controller/                # API控制器
│   │   └── admin/                 # 管理员端控制器
│   ├── dto/                       # 数据传输对象
│   │   ├── request/               # 请求DTO
│   │   └── response/              # 响应DTO
│   ├── config/                    # 配置类
│   ├── security/                  # JWT安全组件
│   └── exception/                 # 异常处理
├── frontend/                      # Vue 3 前端
│   └── src/
│       ├── views/                 # 页面组件
│       │   └── admin/             # 管理员页面
│       ├── layouts/               # 布局组件
│       ├── router/                # 路由配置
│       ├── stores/                # Pinia状态管理
│       └── utils/                 # 工具类
├── ai-service/                    # Python FastAPI AI服务
└── src/main/resources/
    ├── application.yml            # 后端配置
    └── db/init.sql                # 数据库初始化脚本
```

## 快速启动

### 1. 数据库准备
```sql
-- 执行 src/main/resources/db/init.sql
-- 创建数据库 diet_ai_planner 并初始化数据
```

### 2. 启动后端 (Spring Boot)
```bash
# 修改 src/main/resources/application.yml 中的数据库连接信息
./mvnw spring-boot:run
# 后端运行在 http://localhost:8080
```

### 3. 启动前端 (Vue)
```bash
cd frontend
npm install
npm run dev
# 前端运行在 http://localhost:3000
```

### 4. 启动AI服务 (Python)
```bash
cd ai-service
pip install -r requirements.txt
python main.py
# AI服务运行在 http://localhost:8000
```

## 默认账号
- 管理员: admin / admin123

## 技术栈
| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + ECharts + Pinia |
| 后端 | Spring Boot 4.1.1 + Spring Security + JPA + MySQL |
| AI服务 | Python FastAPI |
| 认证 | JWT (HS256) |
| 数据库 | MySQL 8.0 |