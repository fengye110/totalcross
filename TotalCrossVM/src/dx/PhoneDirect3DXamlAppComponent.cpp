#include "Direct3DContentProvider.h"

#include "cppwrapper.h"
#include "../../event/specialkeys.h"

static float glShiftY = 0.0f;

using namespace Windows::Foundation;
using namespace Windows::UI::Core;
using namespace Microsoft::WRL;
using namespace Windows::Phone::Graphics::Interop;
using namespace Windows::Phone::Input::Interop;

namespace PhoneDirect3DXamlAppComponent
{
static Direct3DBackground^ instance;

Direct3DBackground::Direct3DBackground(CSwrapper ^_cs) : cs(_cs)
{
   instance = this;
}

Direct3DBackground^ Direct3DBackground::GetInstance()
{
   return instance;
}
IDrawingSurfaceBackgroundContentProvider^ Direct3DBackground::CreateContentProvider()
{
	ComPtr<Direct3DContentProvider> provider = Make<Direct3DContentProvider>(this);
	return reinterpret_cast<IDrawingSurfaceBackgroundContentProvider^>(provider.Get());
}

bool Direct3DBackground::backKeyPress()
{
	eventQueuePush(KEYEVENT_SPECIALKEY_PRESS, SK_ESCAPE, 0, 0, 0);

	return keepRunning;
}

void Direct3DBackground::SetManipulationHost(DrawingSurfaceManipulationHost^ manipulationHost)
{
}

// Event Handlers
void Direct3DBackground::OnPointerPressed(int x, int y)
{
   eventQueuePush(PENEVENT_PEN_DOWN, 0, x, y - (int)glShiftY, -1);
}

void Direct3DBackground::OnPointerMoved(int x, int y)
{
   eventQueuePush(PENEVENT_PEN_DRAG, 0, x,y - (int)glShiftY, -1);
   isDragging = true;
}

void Direct3DBackground::OnPointerReleased(int x, int y)
{
   eventQueuePush(PENEVENT_PEN_UP, 0, x, y - (int)glShiftY, -1);
	isDragging = false;
}

void Direct3DBackground::OnKeyPressed(int key)
{
   eventQueuePush(key < 32 ? KEYEVENT_SPECIALKEY_PRESS : KEYEVENT_KEY_PRESS, key < 32 ? keyDevice2Portable(key) : key, 0, 0, -1);
}

void Direct3DBackground::OnScreenChanged(int newKeyboardH, int newWidth, int newHeight)
{
   glShiftY = newKeyboardH;
}

// Interface With Direct3DContentProvider
HRESULT Direct3DBackground::Connect(_In_ IDrawingSurfaceRuntimeHostNative* host, _In_ ID3D11Device1* device)
{
   renderer = ref new Direct3DBase(cs);
	renderer->initialize(device);
	renderer->updateForWindowSizeChange(WindowBounds.Width, WindowBounds.Height);
	return S_OK;
}

void Direct3DBackground::Disconnect()
{
	renderer = nullptr;
}

HRESULT Direct3DBackground::PrepareResources(_In_ const LARGE_INTEGER* presentTargetTime, _Inout_ DrawingSurfaceSizeF* desiredRenderTargetSize)
{
	desiredRenderTargetSize->width = RenderResolution.Width;
	desiredRenderTargetSize->height = RenderResolution.Height;
	return S_OK;
}

void Direct3DBackground::RequestNewFrame()
{
   Direct3DBackground::RequestAdditionalFrame();
}

static int lastPaint,lastAcum=10;
HRESULT Direct3DBackground::Draw(_In_ ID3D11Device1* device, _In_ ID3D11DeviceContext1* context, _In_ ID3D11RenderTargetView* renderTargetView)
{
   if (renderer->isLoadCompleted() && renderer->startProgramIfNeeded())
   {
      int cur = (int32)GetTickCount64();
      renderer->updateDevice(device, context, renderTargetView);
      int n = renderer->runCommands();
      //if (--lastAcum == 0) { debug("%d: %d ms", n, cur - lastPaint); lastAcum = 10; } lastPaint = cur;
      if (renderer->alertMsg != nullptr) {Direct3DBase::getLastInstance()->csharp->privateAlertCS(renderer->alertMsg); renderer->alertMsg = nullptr;} // alert stuff
      renderer->updateScreenWaiting = false;
	} 
   else Direct3DBackground::RequestAdditionalFrame();
   return S_OK;
}

}