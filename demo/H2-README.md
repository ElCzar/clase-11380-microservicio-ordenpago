# Configuración de Base de Datos H2

## 📋 Descripción
Este microservicio usa H2 como base de datos en memoria para desarrollo y pruebas.

## 🔧 Configuración

### Configuración Principal (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:cartdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    
  h2:
    console:
      enabled: true
      path: /h2-console
```

## 🚀 Uso

### Acceso a la Consola H2
1. **URL**: `http://localhost:8084/h2-console`
2. **JDBC URL**: `jdbc:h2:mem:cartdb`
3. **Usuario**: `sa`
4. **Contraseña**: `password`

### Ejecutar con Perfil de Desarrollo
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Endpoints de Desarrollo
- `GET /api/dev/database/info` - Información de la base de datos
- `GET /api/dev/database/stats` - Estadísticas de tablas
- `DELETE /api/dev/database/clear` - Limpiar todas las tablas

## 📊 Tablas Creadas Automáticamente

### CARTS
- `id` (UUID) - ID único del carrito
- `user_id` (VARCHAR) - ID del usuario (desde JWT)
- `status` (VARCHAR) - Estado: ACTIVE, COMPLETED, ABANDONED
- `created_at` (TIMESTAMP) - Fecha de creación
- `updated_at` (TIMESTAMP) - Última actualización

### CART_ITEMS
- `id` (UUID) - ID único del item
- `cart_id` (UUID) - Referencia al carrito
- `service_id` (UUID) - ID del servicio
- `service_name` (VARCHAR) - Nombre del servicio (cache)
- `service_price` (DECIMAL) - Precio del servicio (cache)
- `quantity` (INTEGER) - Cantidad
- `added_at` (TIMESTAMP) - Fecha agregado

### PAYMENTS
- `id` (UUID) - ID único del pago
- `cart_id` (UUID) - Referencia al carrito
- `user_id` (VARCHAR) - ID del usuario
- `amount` (DECIMAL) - Monto del pago
- `method` (VARCHAR) - Método: CREDIT_CARD, PAYPAL, BANK_TRANSFER
- `status` (VARCHAR) - Estado: PENDING, COMPLETED, FAILED
- `transaction_id` (VARCHAR) - ID de transacción
- `card_number` (VARCHAR) - Número de tarjeta enmascarado
- `card_holder_name` (VARCHAR) - Nombre del titular
- `processed_at` (TIMESTAMP) - Fecha de procesamiento

## 🧪 Datos de Prueba

### Ejemplo de Consultas SQL
```sql
-- Ver todos los carritos
SELECT * FROM CARTS;

-- Ver items por carrito
SELECT c.user_id, ci.service_name, ci.quantity, ci.service_price
FROM CARTS c 
JOIN CART_ITEMS ci ON c.id = ci.cart_id;

-- Ver pagos completados
SELECT * FROM PAYMENTS WHERE status = 'COMPLETED';

-- Estadísticas de carritos por usuario
SELECT user_id, COUNT(*) as total_carts, SUM(
  (SELECT SUM(quantity * service_price) FROM CART_ITEMS WHERE cart_id = c.id)
) as total_amount
FROM CARTS c
GROUP BY user_id;
```

## 🔄 Reiniciar Base de Datos
La base de datos se recrea automáticamente en cada reinicio de la aplicación cuando `ddl-auto: create-drop` está configurado.

Para limpiar manualmente durante la ejecución:
```bash
curl -X DELETE http://localhost:8084/api/dev/database/clear
```

## 📝 Logs
Con el perfil `dev` activado, verás logs detallados de:
- Todas las consultas SQL ejecutadas
- Parámetros de las consultas
- Estadísticas de Hibernate
- Debug de Spring Security