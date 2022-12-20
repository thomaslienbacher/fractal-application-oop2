# Fractal Renderer Group 109

## Team members

* Lienbacher Thomas
* Marehart Tobias
* Zeba Phillip

## GUI

We implemented everything, except for distributed rendering.
Changing the render mode setting doesn't have an effect.
But connection handling was implemented.
If a connection gets closed or if data can't be sent anymore, we assume
a worker is dead and thus the socket is removed from connected workers.

The local rendering of an image is split into `n` chunks.
More accurately we split it into the number of logical cores,
i.e. `n = Runtime.getRuntime().availableProcessors()`.

Images are rendered interlaced or interleaved, that means that a thread renders
every `n`th line starting at a different offset. This ensures an even spread
of the rendering load.
These are merged into a single large image and copied onto the canvas.

The rendering calculations don't use the expensive `Math.sqrt()` function.
Instead, the squared length is compared against 4.

Explanation: `sqrt(l) <= 2.0` <=> `l^2 <= 4.0`

## Panning and Zooming

The Panning and Zooming were both implemented. The Panning was realized through an initial storing of the coordinates of the mouse upon the first button press,
then afterwards every MouseDragEvent is processed, the delta i.e. the dragged distance is calculated and the image is changed appropriately in the parameters. Upon the end of one DragEvent the coordinates for the
starting position are stored again and the process is repeated until completion. Zooming was implemented in a more simple fashion. With every MouseScrollEvent the innate DeltaY values of the event are taken, multiplied
by the predetermined per-tick increment of 0.02 and then added to/subtracted from the zoom parameter.   

## Server

The FractalApplication is the client, the worker is the server.
Workers are synced every 400ms by sending a ping packet.
If a client disconnects from a worker,
the worker will be able to accept a new client without restarting.

Connection are established via TCP, this brings multiple advantages over UDP.
TCP is ordered and more reliable, it is also connection oriented, which makes
it easier for us to handle the connections.
Blocking IO is used because async IO with nio is complicated ¯\_(ツ)_/¯.

Each worker generates a random id, this was used to differentiate workers
while debugging if multiple are running in a single terminal window.

## Bonus

Two bonus elements were added.
A gray scale render mode that interpolates from black to white
and the GUI displays the render time in seconds.
