{{/*
Expand the name of the chart.
*/}}
{{- define "osb.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "osb.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "osb.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "osb.labels" -}}
helm.sh/chart: {{ include "osb.chart" . }}
{{ include "osb.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "osb.selectorLabels" -}}
app.kubernetes.io/name: {{ include "osb.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "osb.api.fullname" -}}
{{- printf "%s-api" (include "osb.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "osb.bff.fullname" -}}
{{- printf "%s-bff" (include "osb.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "osb.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "osb.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "osb.imageTag" -}}
{{- if .Values.imageTag }}
{{- .Values.imageTag }}
{{- else }}
{{- .Chart.AppVersion }}
{{- end }}
{{- end }}

{{- define "osb.api.image" -}}
{{- $tag := .Values.api.image.tag | default (include "osb.imageTag" .) }}
{{- printf "%s:%s" .Values.api.image.repository $tag }}
{{- end }}

{{- define "osb.bff.image" -}}
{{- $tag := .Values.bff.image.tag | default (include "osb.imageTag" .) }}
{{- printf "%s:%s" .Values.bff.image.repository $tag }}
{{- end }}

{{- define "osb.apiUrl" -}}
{{- if .Values.config.apiUrl }}
{{- .Values.config.apiUrl }}
{{- else }}
{{- printf "http://%s:%v" (include "osb.api.fullname" .) .Values.api.service.port }}
{{- end }}
{{- end }}

{{- define "osb.openbao.fullname" -}}
{{- printf "%s-openbao" (include "osb.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* memory | openbao — empty provider follows openbao.enabled */}}
{{- define "osb.secrets.provider" -}}
{{- if .Values.config.secrets.provider -}}
{{- .Values.config.secrets.provider -}}
{{- else if .Values.openbao.enabled -}}
openbao
{{- else -}}
memory
{{- end -}}
{{- end }}

{{- define "osb.openbao.url" -}}
{{- if .Values.config.secrets.openbao.url -}}
{{- .Values.config.secrets.openbao.url -}}
{{- else if .Values.openbao.enabled -}}
{{- printf "http://%s:%v" (include "osb.openbao.fullname" .) .Values.openbao.service.port -}}
{{- else -}}
{{- default "http://localhost:8200" .Values.config.secrets.openbao.url -}}
{{- end -}}
{{- end }}

{{- define "osb.openbao.token" -}}
{{- if .Values.config.secrets.openbao.token -}}
{{- .Values.config.secrets.openbao.token -}}
{{- else -}}
{{- .Values.openbao.rootToken -}}
{{- end -}}
{{- end }}

{{- define "osb.openbao.keysSecretName" -}}
{{- printf "%s-openbao-keys" (include "osb.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "osb.openbao.tokenSecretName" -}}
{{- if .Values.openbao.existingSecret -}}
{{- .Values.openbao.existingSecret -}}
{{- else if .Values.openbao.devMode -}}
{{- printf "%s-credentials" (include "osb.fullname" .) -}}
{{- else -}}
{{- include "osb.openbao.keysSecretName" . -}}
{{- end -}}
{{- end }}

{{- define "osb.openbao.pgHost" -}}
{{- if .Values.openbao.storage.postgresql.host -}}
{{- .Values.openbao.storage.postgresql.host -}}
{{- else -}}
{{- /* derive host from jdbcUrl jdbc:postgresql://host:port/db */ -}}
{{- $u := .Values.config.postgres.jdbcUrl | trimPrefix "jdbc:" -}}
{{- $withoutScheme := $u | trimPrefix "postgresql://" | trimPrefix "postgres://" -}}
{{- $hostPort := splitList "/" $withoutScheme | first -}}
{{- $host := splitList ":" $hostPort | first -}}
{{- $host -}}
{{- end -}}
{{- end }}

{{- define "osb.openbao.pgConnectionUrl" -}}
{{- if .Values.openbao.storage.postgresql.connectionUrl -}}
{{- .Values.openbao.storage.postgresql.connectionUrl -}}
{{- else -}}
{{- $user := .Values.openbao.storage.postgresql.username | default .Values.config.postgres.username -}}
{{- $pass := .Values.openbao.storage.postgresql.password | default .Values.config.postgres.password -}}
{{- $host := include "osb.openbao.pgHost" . -}}
{{- $port := .Values.openbao.storage.postgresql.port | default 5432 -}}
{{- $db := .Values.openbao.storage.postgresql.database | default "openbao" -}}
{{- $ssl := .Values.openbao.storage.postgresql.sslMode | default "disable" -}}
{{- printf "postgres://%s:%s@%s:%v/%s?sslmode=%s" $user $pass $host $port $db $ssl -}}
{{- end -}}
{{- end }}

{{- define "osb.openbao.apiAddr" -}}
{{- printf "http://%s:%v" (include "osb.openbao.fullname" .) .Values.openbao.service.port -}}
{{- end }}