﻿#pragma once

#include "DirectXHelper.h"
#include "cswrapper.h"

#define HAS_TCHAR
#include "tcvm.h"

#define TASKS_COMPLETED ((1 << 6)-1) // 6 tasks

struct ProjectionConstantBuffer
{
   DirectX::XMFLOAT4X4 projection;
};

struct VertexPosition
{
   DirectX::XMFLOAT2 pos;
};

struct VertexColor
{
   DirectX::XMFLOAT4 color;
};

struct VertexPositionColor
{
   DirectX::XMFLOAT2 pos;
   DirectX::XMFLOAT4 color;
};


struct TextureVertex
{
   DirectX::XMFLOAT2 pos;  // position
   DirectX::XMFLOAT2 tex;  // texture coordinate
};

enum whichProgram
{
   PROGRAM_NONE,
   PROGRAM_GC,  // global color
   PROGRAM_TEX, // texture
   PROGRAM_LC,  // local color
};

#include "tcthread.h"

// Helper class that initializes DirectX APIs for 3D rendering.
ref class Direct3DBase 
{
internal:
   Direct3DBase(PhoneDirect3DXamlAppComponent::CSwrapper ^_cs);

   Platform::String^ alertMsg;
   PhoneDirect3DXamlAppComponent::CSwrapper ^csharp;

	bool updateScreenRequested;
   bool eventsInitialized;
	void Initialize(_In_ ID3D11Device1* device);
	void CreateDeviceResources();
	void UpdateDevice(_In_ ID3D11Device1* device, _In_ ID3D11DeviceContext1* context, _In_ ID3D11RenderTargetView* renderTargetView);
	void CreateWindowSizeDependentResources();
	void UpdateForWindowSizeChange(float width, float height);
	void PreRender(); // resets the screen and set it ready to render
   void startVMIfNeeded();
	void updateScreen();

   void setProgram(whichProgram p);
   void loadTexture(Context currentContext, TCObject img, int32* textureId, Pixel *pixels, int32 width, int32 height, bool updateList);
   void deleteTexture(TCObject img, int32* textureId, bool updateList);
   void drawTexture(int32* textureId, int32 x, int32 y, int32 w, int32 h, int32 dstX, int32 dstY, int32 imgW, int32 imgH, PixelConv *color, int32* clip);
   void drawLine(int x1, int y1, int x2, int y2, int color);
   void drawPixels(int *x, int *y, int count, int color);
   void fillRect(int x1, int y1, int x2, int y2, int color);
   void fillShadedRect(int32 x, int32 y, int32 w, int32 h, PixelConv c1, PixelConv c2, bool horiz);
   void setColor(int color);
   void createTexture();
   void setup();

   bool isLoadCompleted();

   static Direct3DBase ^GetLastInstance();

private:
   int loadCompleted;
   whichProgram curProgram;
   int lastRGB;
   float aa, rr, gg, bb;
   ID3D11Buffer *pBufferRect, *pBufferPixels, *pBufferColor, *texVertexBuffer, *pBufferRectLC;
   int lastPixelsCount;
   D3D11_RECT clipRect;
   bool clipSet;

   VertexPosition *pixelsVertices;

   // texture
   Microsoft::WRL::ComPtr<ID3D11SamplerState> texsampler;
   ID3D11DepthStencilState* depthDisabledStencilState;
   ID3D11BlendState* g_pBlendState;

   Microsoft::WRL::ComPtr<ID3D11SamplerState> sampler;
   Microsoft::WRL::ComPtr<ID3D11InputLayout> m_inputLayout, m_inputLayoutT, m_inputLayoutLC;
   Microsoft::WRL::ComPtr<ID3D11Buffer> m_vertexBuffer;
   Microsoft::WRL::ComPtr<ID3D11Buffer> m_indexBuffer, pixelsIndexBuffer, colorBuffer;
   Microsoft::WRL::ComPtr<ID3D11VertexShader> m_vertexShader, m_vertexShaderT, m_vertexShaderLC;
   Microsoft::WRL::ComPtr<ID3D11PixelShader> m_pixelShader, m_pixelShaderT, m_pixelShaderLC;
   Microsoft::WRL::ComPtr<ID3D11Buffer> m_constantBuffer;

   ProjectionConstantBuffer m_constantBufferData;
   ID3D11RasterizerState1 *pRasterStateEnableClipping, *pRasterStateDisableClipping;

protected private:
	// Direct3D Objects.
	Microsoft::WRL::ComPtr<ID3D11Device1> m_d3dDevice;
	Microsoft::WRL::ComPtr<ID3D11DeviceContext1> m_d3dContext;
	Microsoft::WRL::ComPtr<ID3D11RenderTargetView> m_renderTargetView;
	Microsoft::WRL::ComPtr<ID3D11DepthStencilView> m_depthStencilView;

	// Cached renderer properties.
	Windows::Foundation::Size m_renderTargetSize;
	Windows::Foundation::Rect m_windowBounds;

	// TotalCross objects
	Context local_context;
	bool VMStarted;

	// DrawCommand internal variables
};
