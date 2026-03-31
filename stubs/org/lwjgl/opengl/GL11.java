package org.lwjgl.opengl;
public class GL11 {
    public static final int GL_TEXTURE_2D   = 0x0DE1;
    public static final int GL_DEPTH_TEST   = 0x0B71;
    public static final int GL_BLEND        = 0x0BE2;
    public static final int GL_ALPHA_TEST   = 0x0BC0;
    public static final int GL_SCISSOR_TEST = 0x0C11;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_MODELVIEW = 0x1700;
    public static final int GL_LIGHTING = 0x0B50;
    public static final int GL_COLOR_MATERIAL = 0x0B57;
    public static void glEnable(int cap) {}
    public static void glDisable(int cap) {}
    public static void glPushMatrix() {}
    public static void glPopMatrix() {}
    public static void glTranslatef(float x, float y, float z) {}
    public static void glScalef(float x, float y, float z) {}
    public static void glRotatef(float angle, float x, float y, float z) {}
    public static void glColor4f(float r, float g, float b, float a) {}
    public static void glColor3f(float r, float g, float b) {}
    public static void glClear(int mask) {}
    public static void glBlendFunc(int sFactor, int dFactor) {}
    public static void glAlphaFunc(int func, float ref) {}
    public static void glScissor(int x, int y, int width, int height) {}
    public static void glLineWidth(float width) {}
    public static void glDepthMask(boolean flag) {}
}
