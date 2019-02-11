import { User } from '../models/user';
import { Request, Response } from 'express';
import * as bcrypt from 'bcryptjs';
import * as jwt from 'jsonwebtoken';
import {AuthController} from "../security/auth-controller";

export class LogInController{

    constructor() {}

    public login(req: Request, res: Response): void {
        User.findOne({email: req.body.email}, (err: any, user: Document) => {
            if (err) {
                res.status(500).send(err);
            } else {
                // check password
                // @ts-ignore
                bcrypt.compare(req.body.password, user.password)
                    .then(value => {
                        if (value) {
                            // create a token
                            // @ts-ignore
                            const token = jwt.sign({ id: user._id }, AuthController.secret, {
                                expiresIn: 86400 // expires in 24 hours
                            });
                            res.status(200).json({auth: true, message: 'Success'});
                        } else {
                            res.status(401).json({auth: false, message: 'Bad credentials'});
                        }
                    })
                    .catch(reason => {
                        res.status(401).send(reason);
                    });
            }
        });
    }

    public logout(req: Request, res: Response) {
        // remains to be seen
        res.status(405).json({auth: false, message: 'Not implemented yet!'});
    }
}
