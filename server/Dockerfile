FROM python:3.12-slim
WORKDIR /app
COPY server/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r requirements.txt && useradd --create-home alpi && mkdir -p /app/data && chown alpi:alpi /app/data
COPY server /app/server
USER alpi
ENV ALPI_DATA=/app/data
EXPOSE 8000
CMD ["uvicorn", "server.app:app", "--host", "0.0.0.0", "--port", "8000", "--no-proxy-headers"]
