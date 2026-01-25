# OpShare

A peer-to-peer file sharing application built with Spring Boot that enables secure file sharing between users through rooms with real-time notifications.

## Features

- **Room-based File Sharing**: Create or join rooms to share files with specific groups
- **Secure Authentication**: JWT-based authentication with OTP verification via Twilio
- **File Upload/Download**: Upload files to AWS S3 with chunked upload support
- **Real-time Notifications**: WebSocket-based real-time updates for file operations
- **File Access Control**: Offer/accept/reject mechanism for file sharing
- **Multi-device Support**: Device-based login system for seamless access
- **Peer Management**: Register and manage peers within rooms

## Tech Stack

- **Backend**: Spring Boot 4.0.2, Java 17
- **Database**: PostgreSQL with JPA/Hibernate
- **Cache**: Redis
- **File Storage**: AWS S3
- **Authentication**: JWT with Twilio OTP
- **Real-time**: WebSocket
- **Message Queue**: Apache Kafka
- **Build Tool**: Gradle

## Prerequisites

- Java 17+
- PostgreSQL
- Redis
- AWS S3 bucket
- Twilio account (for OTP)

## Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd OpShare
   ```

2. **Set up environment variables**
   ```bash
   export AWS_ACCESS_KEY_ID=your_aws_access_key
   export AWS_SECRET_ACCESS_KEY=your_aws_secret_key
   export JWT_SECRET=your_jwt_secret_key
   export TWILIO_ACCOUNT_SID=your_twilio_account_sid
   export TWILIO_AUTH_TOKEN=your_twilio_auth_token
   export TWILIO_VERIFY_SERVICE_SID=your_twilio_verify_service_sid
   ```

3. **Configure database**
   - Create a PostgreSQL database named `postgres`
   - Update `application.properties` with your database credentials

4. **Start Redis server**
   ```bash
   redis-server
   ```

5. **Build and run the application**
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /auth/send-otp` - Send OTP for phone verification
- `POST /auth/verify-otp` - Verify OTP and register user
- `POST /auth/login` - Login with credentials
- `POST /auth/device-login` - Device-based login

### Rooms
- `POST /rooms` - Create a new room
- `POST /rooms/{roomId}/join` - Join a room
- `POST /rooms/{roomId}/leave` - Leave a room
- `GET /rooms/{roomId}/peers` - Get room peers

### Files
- `POST /files/upload/{roomId}` - Upload file to room
- `POST /files/offer` - Offer file to peer
- `POST /files/{fileId}/accept` - Accept file offer
- `POST /files/{fileId}/reject` - Reject file offer
- `GET /files/{fileId}/download` - Get download URL
- `GET /files/room/{roomId}` - Get files in room
- `GET /files/pending-offers` - Get pending file offers
- `DELETE /files/{fileId}` - Delete file

### Chunked Upload
- `POST /chunked-upload/init` - Initialize chunked upload
- `POST /chunked-upload/chunk` - Upload file chunk
- `POST /chunked-upload/complete` - Complete chunked upload

### Peers
- `POST /peers/register` - Register peer
- `PUT /peers/{peerId}` - Update peer info
- `GET /peers` - Get all peers

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=1234

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# AWS S3
aws.s3.bucket-name=opshare-files
aws.s3.region=ap-south-1
aws.s3.presigned-url-expiration-minutes=260

# JWT
jwt.expiration-hours=24
```

## WebSocket Events

The application supports real-time communication through WebSocket:

- File upload progress
- Room events (join/leave)
- File offer notifications
- Peer status updates

## Project Structure

```
src/main/java/com/example/OpShare/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data transfer objects
├── entity/         # JPA entities
├── repository/     # Data repositories
├── service/        # Business logic
└── exception/      # Exception handlers
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.
