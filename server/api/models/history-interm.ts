class HistoryInterm {
    public trackerId: string;
    public lat: number;
    public lng: number;
    public range: number;
    public source: string;
}

class DecodedPosition {
    public location: DecodedData;
    public accuracy: number;
}

class DecodedData {
    public lat: number;
    public lng: number;
}

export {HistoryInterm, DecodedPosition};