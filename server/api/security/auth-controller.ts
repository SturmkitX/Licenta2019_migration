import * as jwt from 'jsonwebtoken';
import {Request, Response} from 'express';

export class AuthController {

    public static readonly secret: string = 'neverfound';

    constructor() {}

    public static verifyToken(req: Request, res: Response, next) {
        console.log(req.url || 'Empty');
        // don't verify token if the link is either /login or /logout
        if (req.url === '/login' || req.url === '/logout') {
            next();
        }

        // @ts-ignore
        let token: string = req.headers['x-access-token'];
        if (!token) {
            return res.status(401).send({ auth: false, message: 'No token provided.' });
        }

        jwt.verify(token, this.secret, (err, decoded) => {
            if (err) {
                return res.status(500)
                    .send({ auth: false, message: 'Failed to authenticate token.' });
            }
            next();
        });
    }


}