FROM bitnami/spark:3.3.0
COPY target/scala-2.12/spark-graphx-0.1.0.jar .
COPY driver-jars/postgresql-42.3.3.jar .
COPY log4j2.xml $SPARK_HOME/conf
# dodać --driver-class-path postgresql-9.4.1207.jar
CMD ["./bin/spark-submit", "--master", "local[2]", "--packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.3.0,org.apache.spark:spark-sql_2.12:3.3.0,org.slf4j:slf4j-nop:2.0.5,org.postgresql:postgresql:42.3.3", "--driver-class-path", "postgresql-42.3.3.jar", "--class", "io.github.malyszaryczlowiek.SparkGraphX", "/opt/bitnami/spark/spark-graphx-0.1.0.jar", "1000"]
