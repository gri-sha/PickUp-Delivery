from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.responses import FileResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import os
from typing import List, Dict
from models import DeliveryRequest, CourierRoute
from tsp_solver import solve_tsp
from utils import get_xml_files, PLANS_FOLDER, REQUESTS_FOLDER
from utils import load_plan_graph

app = FastAPI()

# CORS middleware - must be added before routes
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/request-names")
async def get_request_names():
    requests = get_xml_files(REQUESTS_FOLDER)
    return requests


@app.post("/upload-request")
async def upload_request(file: UploadFile = File(...)):
    try:
        # Ensure filename has .xml extension
        filename = file.filename
        if not filename.endswith(".xml"):
            filename += ".xml"

        # Save file to requests folder
        file_path = os.path.join(REQUESTS_FOLDER, filename)

        # Read file content and save
        content = await file.read()
        with open(file_path, "wb") as f:
            f.write(content)

        return {"message": "File uploaded successfully", "filename": filename}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
@app.get("/plan-names")
async def get_plan_names():
    plans = get_xml_files(PLANS_FOLDER)
    return plans


@app.get("/plans/{filename}")
async def get_plan_file(filename: str):
    # Check if it has .xml extension, if not add it
    if not filename.endswith(".xml"):
        filename += ".xml"

    plan_path = os.path.join(PLANS_FOLDER, filename)
    if os.path.exists(plan_path):
        return FileResponse(plan_path, media_type="application/xml", filename=filename)

    raise HTTPException(status_code=404, detail="Plan file not found")


@app.get("/requests/{filename}")
async def get_request_file(filename: str):
    # Check if it has .xml extension, if not add it
    if not filename.endswith(".xml"):
        filename += ".xml"

    request_path = os.path.join(REQUESTS_FOLDER, filename)
    if os.path.exists(request_path):
        return FileResponse(
            request_path, media_type="application/xml", filename=filename
        )

    raise HTTPException(status_code=404, detail="Request file not found")


@app.get("/get_tsp", response_model=list[CourierRoute])
async def post_delivery(request: DeliveryRequest):
    try:
        return solve_tsp(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8080)
