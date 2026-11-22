# Spec Delta

## Purpose

Definir validaciones portables y deterministas para identidad mínima de pacientes, evitando duplicados por representaciones diferentes de DNI.

## ADDED Requirements

### Requirement: El DNI debe normalizarse antes de validar o persistir
El sistema SHALL eliminar puntos y espacios de Patient.dni, rechazar letras y otros caracteres no numéricos y conservar únicamente 7 u 8 dígitos.

#### Scenario: El DNI contiene puntos y espacios
- **WHEN** se recibe 27.123.456 o 27 123 456
- **THEN** ambos formatos se normalizan a 27123456

#### Scenario: El DNI contiene letras
- **WHEN** se recibe una cadena con letras o símbolos
- **THEN** el dato se rechaza como inválido

#### Scenario: El DNI normalizado tiene longitud inválida
- **WHEN** el DNI contiene menos de 7 o más de 8 dígitos
- **THEN** el dato se rechaza

### Requirement: El DNI debe ser único después de normalización
El modelo SHALL almacenar el DNI normalizado y SHALL aplicar unicidad sobre su representación persistida.

#### Scenario: Se crean dos pacientes con DNI equivalente
- **WHEN** dos entradas representan el mismo DNI con formatos distintos pero dígitos iguales
- **THEN** solo puede existir una identidad persistente por DNI normalizado

### Requirement: El email debe cumplir un formato estándar
Patient.email SHALL validar formato estándar de dirección de correo antes de persistirse.

#### Scenario: Se recibe un email válido
- **WHEN** el valor contiene local, separador @ y dominio válidos
- **THEN** el email puede persistirse

#### Scenario: Se recibe un email inválido
- **WHEN** falta @, falta dominio o existe un formato claramente inválido
- **THEN** el dato se rechaza
