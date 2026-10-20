uniform sampler2D DiffuseSampler0;
uniform sampler2D PanelSampler1;
uniform sampler2D PanelSampler2;
uniform sampler2D PanelSampler3;
uniform sampler2D PanelSampler4;

#define MAX_PANELS 4
#define MAX_EDGES 6

uniform int   PanelCount;
uniform int   PanelTex[MAX_PANELS];
uniform int   EdgeCount[MAX_PANELS];
uniform float EdgeNX[MAX_PANELS * MAX_EDGES];
uniform float EdgeNY[MAX_PANELS * MAX_EDGES];
uniform float EdgeC[MAX_PANELS * MAX_EDGES];
uniform float Aspect;
uniform float BorderWidth;
uniform vec4  BorderColor;
uniform vec4  GapColor;

in  vec2 texCoord;
out vec4 fragColor;

vec4 sampleView(int t, vec2 uv) {
    if (t == 1) return texture(PanelSampler1, uv);
    if (t == 2) return texture(PanelSampler2, uv);
    if (t == 3) return texture(PanelSampler3, uv);
    if (t == 4) return texture(PanelSampler4, uv);
    return texture(DiffuseSampler0, uv);
}

void main() {
    if (PanelCount == 0) {
        fragColor = texture(DiffuseSampler0, texCoord);
        return;
    }

    vec2 p = vec2(texCoord.x * Aspect, texCoord.y);
    int   hit = -1;
    float edgeDist = 1.0e9;

    for (int i = 0; i < PanelCount; i++) {
        bool  inside = true;
        float dmin = 1.0e9;
        int   n = EdgeCount[i];
        for (int e = 0; e < MAX_EDGES; e++) {
            if (e >= n) break;
            int   k = i * MAX_EDGES + e;
            float d = EdgeNX[k] * p.x + EdgeNY[k] * p.y + EdgeC[k];
            if (d < 0.0) {
                inside = false;
                break;
            }
            dmin = min(dmin, d);
        }
        if (inside) {
            hit = i;
            edgeDist = dmin;
        }
    }

    vec4 color = GapColor;
    if (hit >= 0) {
        color = sampleView(PanelTex[hit], texCoord);
        if (BorderWidth > 0.0 && edgeDist < BorderWidth) {
            float k = smoothstep(BorderWidth, BorderWidth * 0.7, edgeDist);
            color = mix(color, BorderColor, BorderColor.a * k);
        }
    }
    fragColor = vec4(color.rgb, 1.0);
}
