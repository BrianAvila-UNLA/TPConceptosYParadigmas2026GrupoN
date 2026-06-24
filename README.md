# Intérprete del Lenguaje Simple

### Integrantes del Grupo
* Brian Avila - 44620619
* Diego Aguirre - 

### Variante Asignada
* **Estructura de Control Iterativa:** Bucle `do-while`.

## Descripción del Lenguaje
`Simple` es un lenguaje de programación imperativo de tipado dinámico diseñado sobre **ANTLR4** y **Java**. El entorno procesa scripts con extensión `.smp` ejecutando un análisis en cascada (léxico, sintáctico y semántico) para interpretar instrucciones en tiempo real. Se utilizaron los videos guía e información en la red para el desarrollo del mismo.

### Características Principales:
* **Variables y Ámbito:** Declaración obligatoria mediante la palabra clave `var` y asignación dinámica de valores a través del operador `=`.
* **Flujo de Control:** Soporte nativo para bifurcaciones condicionales completas (`if-else`) y repeticiones garantizadas de al menos una iteración (`do-while`), ambas estructuras protegidas por bloques de llaves `{}`.
* **Consola:** Salida estándar de datos por terminal utilizando la función `println`.
* **Comentarios:** El compilador ignora de forma nativa comentarios de una sola línea (`//`) y comentarios de bloque (`/* ... */`).
* **Tipos:** El compilador puede procesar
* **Funciones:** El compilador puede declarar variables, asignar variables, imprimir linea, condicional y hacer-mientras.

---

## Decisiones de Diseño

1. **Patrón Visitor para Interpretación Directa:** Tras evaluar arquitecturas intermedias, se optó por un enfoque clásico y directo extendiendo la clase `SimpleBaseVisitor<Object>`. Al principio, se construyó un árbol de sintaxis abstracta (AST) intermedio para ir entendiendo el funcionamiento del mismo, pero fue posteriormente descartado por `SimpleCustomVisitor`, quien ejecuta las acciones semánticas y evalúa las expresiones directamente mientras recorre los nodos generados por ANTLR, cosa que pedía el enunciado.
2. **Tabla de Símbolos Dinámica:** El estado del programa y el valor de las variables se administran en tiempo de ejecución mediante un mapa centralizado (`Map<String, Object> symbolTable`) dentro del Visitor. Al no requerir tipado estricto en la declaración, el tipo de la variable se infiere directamente del valor evaluado en la asignación.
3. **Manejo de Precedencia en la Gramática:** Para evitar ambigüedades matemáticas sin sobrecargar al Visitor, la jerarquía de operaciones se resolvió directamente en las reglas del archivo `Simple.g4`. La estructura encadena las expresiones de menor a mayor relevancia:
   `comparison` ➡️ `logicalOr` ➡️ `logicalAnd` ➡️ `expression` (suma/resta) ➡️ `term` (multiplicación/división) ➡️ `factor`. Esto garantiza que `2 + 3 * 4` devuelva correctamente `14` de forma nativa.
4. **Validaciones Semánticas y Enfoque Fail-Fast:** El intérprete valida activamente las reglas del negocio del lenguaje lanzando excepciones del tipo `RuntimeException`. Implementa un enfoque *Fail-Fast* (frenado inmediato) que interrumpe la ejecución al detectar el primer error crítico (como la división por cero o el uso de variables no declaradas/duplicadas) para proteger la integridad de los datos en la memoria.

---

### Prerrequisitos
* **Java JDK:** Versión 24.0.2.
* **Apache Maven:** Integrado en el IDE (Eclipse) o mediante terminal de comandos.
* **ANTLR:** Versión 4.13.1.

* ### 1. Limpieza y Generación de Fuentes de ANTLR
Para limpiar binarios previos y obligar al plugin de Maven a compilar la gramática `.g4` generando un Lexer y Parser compatibles con la versión actual (4.13.1), ejecute:
```bash
mvn clean generate-sources
