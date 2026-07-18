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
